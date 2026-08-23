<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as THREE from 'three'

export interface PlanetInfo {
  id: string
  name: string
  nameEn: string
  blurb: string
  color: string
}

const emit = defineEmits<{
  select: [planet: PlanetInfo | null]
}>()

const host = ref<HTMLDivElement | null>(null)
const useCanvas = ref(false)

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationId = 0
let disposed = false
let resizeObserver: ResizeObserver | null = null
let pointerHandler: ((e: PointerEvent) => void) | null = null
let clickHandler: ((e: MouseEvent) => void) | null = null
const disposables: Array<THREE.BufferGeometry | THREE.Material | THREE.Texture> = []
const raycaster = new THREE.Raycaster()
const pointer = new THREE.Vector2()

interface PlanetDef {
  id: string
  name: string
  nameEn: string
  blurb: string
  base: string
  accent: string
  radius: number
  distance: number
  orbitSpeed: number
  spinSpeed: number
  ring?: boolean
  gas?: boolean
  clouds?: boolean
}

interface PlanetBody {
  def: PlanetDef
  pivot: THREE.Object3D
  mesh: THREE.Mesh
  atmosphere: THREE.Mesh
  clouds?: THREE.Mesh
  distance: number
  orbitSpeed: number
  spinSpeed: number
  angle: number
  pulse: number
}

const PLANETS: PlanetDef[] = [
  { id: 'mercury', name: '水星', nameEn: 'MERCURY', blurb: '灼热与极寒并存的先驱世界。', base: '#6e6a64', accent: '#c8c2b8', radius: 0.36, distance: 5.2, orbitSpeed: 1.4, spinSpeed: 0.01 },
  { id: 'venus', name: '金星', nameEn: 'VENUS', blurb: '厚重大气包裹的炽热星球。', base: '#c4a36a', accent: '#f0d9a8', radius: 0.7, distance: 7.2, orbitSpeed: 1.05, spinSpeed: 0.006 },
  { id: 'earth', name: '地球', nameEn: 'EARTH', blurb: '生命与工程的交汇点，auto-soft 从这里起飞。', base: '#1a4f8a', accent: '#3dbf7a', radius: 0.76, distance: 9.4, orbitSpeed: 0.82, spinSpeed: 0.02, clouds: true },
  { id: 'mars', name: '火星', nameEn: 'MARS', blurb: '锈红地表与稀薄大气，多行星时代的下一站。', base: '#8a2e18', accent: '#e87848', radius: 0.56, distance: 11.8, orbitSpeed: 0.66, spinSpeed: 0.018 },
  { id: 'jupiter', name: '木星', nameEn: 'JUPITER', blurb: '气态巨行星，风暴层叠如系统吞吐峰值。', base: '#c9a57a', accent: '#8b5a3c', radius: 1.5, distance: 15.6, orbitSpeed: 0.34, spinSpeed: 0.04, gas: true },
  { id: 'saturn', name: '土星', nameEn: 'SATURN', blurb: '光环如权限边界，华丽却精密。', base: '#d7c39a', accent: '#b89b6a', radius: 1.24, distance: 19.4, orbitSpeed: 0.24, spinSpeed: 0.035, ring: true, gas: true },
]

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function track<T extends THREE.BufferGeometry | THREE.Material | THREE.Texture>(item: T): T {
  disposables.push(item)
  return item
}

function hash(x: number, y: number) {
  const s = Math.sin(x * 127.1 + y * 311.7) * 43758.5453
  return s - Math.floor(s)
}

function noise(x: number, y: number) {
  const xi = Math.floor(x)
  const yi = Math.floor(y)
  const xf = x - xi
  const yf = y - yi
  const u = xf * xf * (3 - 2 * xf)
  const v = yf * yf * (3 - 2 * yf)
  return (
    hash(xi, yi) * (1 - u) * (1 - v) +
    hash(xi + 1, yi) * u * (1 - v) +
    hash(xi, yi + 1) * (1 - u) * v +
    hash(xi + 1, yi + 1) * u * v
  )
}

function fbm(x: number, y: number, octaves = 6) {
  let value = 0
  let amp = 0.5
  let freq = 1
  for (let i = 0; i < octaves; i += 1) {
    value += amp * noise(x * freq, y * freq)
    amp *= 0.5
    freq *= 2.1
  }
  return value
}

function hexToRgb(hex: string) {
  const n = parseInt(hex.slice(1), 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

function createNebulaTexture() {
  const size = 1024
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  const grad = ctx.createRadialGradient(size * 0.55, size * 0.45, size * 0.05, size * 0.5, size * 0.5, size * 0.55)
  grad.addColorStop(0, '#1a2848')
  grad.addColorStop(0.35, '#0a1020')
  grad.addColorStop(1, '#03050c')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, size, size)
  for (let i = 0; i < 8000; i += 1) {
    const x = Math.random() * size
    const y = Math.random() * size
    const a = Math.random() * 0.35
    const r = Math.random() * 1.2
    ctx.fillStyle = `rgba(200,220,255,${a})`
    ctx.beginPath()
    ctx.arc(x, y, r, 0, Math.PI * 2)
    ctx.fill()
  }
  const tex = track(new THREE.CanvasTexture(canvas))
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

function paintPlanetPixels(def: PlanetDef, size: number) {
  const data = new Uint8ClampedArray(size * size * 4)
  const bump = new Float32Array(size * size)
  const base = hexToRgb(def.base)
  const accent = hexToRgb(def.accent)

  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const u = x / size
      const v = y / size
      const lat = Math.abs(v - 0.5) * 2
      const n1 = fbm(u * 12 + def.id.length, v * 12, 6)
      const n2 = fbm(u * 28 + 3, v * 28 + 7, 4)
      const n3 = fbm(u * 64, v * 64, 3)
      const height = n1 * 0.55 + n2 * 0.3 + n3 * 0.15
      const i = (y * size + x) * 4
      let r = base.r
      let g = base.g
      let b = base.b

      if (def.id === 'earth') {
        const land = height > 0.48
        const coast = Math.abs(height - 0.48) < 0.03
        if (land) {
          r = 34 + height * 90
          g = 95 + height * 80
          b = 48 + height * 40
        } else {
          r = 8 + height * 30
          g = 45 + height * 60
          b = 110 + height * 90
        }
        if (coast) {
          r = 180
          g = 210
          b = 230
        }
        const ice = lat > 0.88
        if (ice) {
          r = 220
          g = 235
          b = 245
        }
      } else if (def.id === 'mars') {
        r = 95 + height * 110
        g = 35 + height * 50
        b = 20 + height * 25
        if (lat > 0.82) {
          r = 210
          g = 200
          b = 190
        }
        const night = u > 0.56 && u < 0.74
        if (night && noise(u * 80, v * 80) > 0.68) {
          r = 255
          g = 200
          b = 120
        }
      } else if (def.id === 'jupiter') {
        const band = Math.sin(v * Math.PI * 14 + Math.sin(u * 8) * 0.4)
        r = 180 + band * 40 + height * 30
        g = 140 + band * 25 + height * 20
        b = 100 + band * 15
        const spot = Math.hypot(u - 0.62, v - 0.38) < 0.06
        if (spot) {
          r = 190
          g = 70
          b = 50
        }
      } else if (def.id === 'saturn') {
        const band = Math.sin(v * Math.PI * 10)
        r = 210 + band * 20
        g = 185 + band * 15
        b = 140 + band * 10
      } else if (def.gas) {
        const mix = height
        r = base.r * (1 - mix) + accent.r * mix
        g = base.g * (1 - mix) + accent.g * mix
        b = base.b * (1 - mix) + accent.b * mix
      } else {
        const crater = Math.max(0, 0.5 - Math.hypot(height - 0.4, noise(u * 22, v * 22) - 0.5))
        const mix = Math.min(1, height * 0.8 + crater * 0.6)
        r = base.r * (1 - mix) + accent.r * mix
        g = base.g * (1 - mix) + accent.g * mix
        b = base.b * (1 - mix) + accent.b * mix
      }

      data[i] = r
      data[i + 1] = g
      data[i + 2] = b
      data[i + 3] = 255
      bump[y * size + x] = height
    }
  }
  return { data, bump }
}

function createPlanetMaps(def: PlanetDef) {
  const size = 1024
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  const { data, bump } = paintPlanetPixels(def, size)
  ctx.putImageData(new ImageData(data, size, size), 0, 0)

  const map = track(new THREE.CanvasTexture(canvas))
  map.colorSpace = THREE.SRGBColorSpace
  map.anisotropy = 8

  const bumpCanvas = document.createElement('canvas')
  bumpCanvas.width = size
  bumpCanvas.height = size
  const bumpCtx = bumpCanvas.getContext('2d')!
  const bumpImg = bumpCtx.createImageData(size, size)
  for (let i = 0; i < bump.length; i += 1) {
    const v = Math.floor(bump[i] * 255)
    bumpImg.data[i * 4] = v
    bumpImg.data[i * 4 + 1] = v
    bumpImg.data[i * 4 + 2] = v
    bumpImg.data[i * 4 + 3] = 255
  }
  bumpCtx.putImageData(bumpImg, 0, 0)
  const bumpMap = track(new THREE.CanvasTexture(bumpCanvas))

  return { map, bumpMap }
}

function createCloudTexture() {
  const size = 512
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  const img = ctx.createImageData(size, size)
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const u = x / size
      const v = y / size
      const n = fbm(u * 8, v * 8, 5)
      const a = n > 0.52 ? (n - 0.52) * 420 : 0
      const i = (y * size + x) * 4
      img.data[i] = 255
      img.data[i + 1] = 255
      img.data[i + 2] = 255
      img.data[i + 3] = Math.min(255, a)
    }
  }
  ctx.putImageData(img, 0, 0)
  const tex = track(new THREE.CanvasTexture(canvas))
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

function createRingTexture() {
  const size = 512
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')!
  const img = ctx.createImageData(size, size)
  const cx = size / 2
  const cy = size / 2
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const dist = Math.hypot(x - cx, y - cy) / (size / 2)
      const band = fbm(dist * 12, Math.atan2(y - cy, x - cx) * 3, 4)
      let a = 0
      if (dist > 0.42 && dist < 0.92) {
        a = (0.35 + band * 0.45) * (1 - Math.abs(dist - 0.68) * 2.5)
      }
      const i = (y * size + x) * 4
      const c = 210 + band * 30
      img.data[i] = c
      img.data[i + 1] = 185 + band * 20
      img.data[i + 2] = 140 + band * 15
      img.data[i + 3] = Math.max(0, Math.min(255, a * 255))
    }
  }
  ctx.putImageData(img, 0, 0)
  const tex = track(new THREE.CanvasTexture(canvas))
  tex.colorSpace = THREE.SRGBColorSpace
  return tex
}

function createAtmosphereMaterial(color: THREE.Color, intensity = 0.35) {
  return track(
    new THREE.ShaderMaterial({
      uniforms: {
        glowColor: { value: color },
        intensity: { value: intensity },
      },
      vertexShader: `
        varying vec3 vNormal;
        varying vec3 vView;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          vec4 mv = modelViewMatrix * vec4(position, 1.0);
          vView = normalize(-mv.xyz);
          gl_Position = projectionMatrix * mv;
        }
      `,
      fragmentShader: `
        uniform vec3 glowColor;
        uniform float intensity;
        varying vec3 vNormal;
        varying vec3 vView;
        void main() {
          float fresnel = pow(1.0 - max(dot(vNormal, vView), 0.0), 2.8);
          float alpha = fresnel * intensity;
          gl_FragColor = vec4(glowColor, alpha);
        }
      `,
      transparent: true,
      side: THREE.BackSide,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  )
}

function createStars() {
  const count = 3500
  const positions = new Float32Array(count * 3)
  const sizes = new Float32Array(count)
  for (let i = 0; i < count; i += 1) {
    const radius = 120 + Math.random() * 200
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = radius * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = radius * Math.cos(phi)
    sizes[i] = 0.15 + Math.random() * 0.55
  }
  const geometry = track(new THREE.BufferGeometry())
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  geometry.setAttribute('size', new THREE.BufferAttribute(sizes, 1))
  const material = track(
    new THREE.PointsMaterial({
      size: 0.4,
      transparent: true,
      opacity: 0.95,
      color: 0xe8eeff,
      sizeAttenuation: true,
      depthWrite: false,
      blending: THREE.AdditiveBlending,
    }),
  )
  return new THREE.Points(geometry, material)
}

function createOrbit(radius: number) {
  const curve = new THREE.EllipseCurve(0, 0, radius, radius * 0.9, 0, Math.PI * 2)
  const points = curve.getPoints(200).map((p) => new THREE.Vector3(p.x, 0, p.y))
  const geometry = track(new THREE.BufferGeometry().setFromPoints(points))
  const material = track(
    new THREE.LineBasicMaterial({ color: 0x4a5a7a, transparent: true, opacity: 0.12 }),
  )
  return new THREE.LineLoop(geometry, material)
}

function createPlanet(def: PlanetDef) {
  const { map, bumpMap } = createPlanetMaps(def)
  const segments = 128
  const geometry = track(new THREE.SphereGeometry(def.radius, segments, segments))
  const material = track(
    new THREE.MeshPhysicalMaterial({
      map,
      bumpMap,
      bumpScale: def.gas ? 0.02 : 0.08,
      roughness: def.gas ? 0.65 : 0.82,
      metalness: def.gas ? 0.05 : 0.02,
      clearcoat: def.gas ? 0.35 : 0.1,
      clearcoatRoughness: 0.4,
      emissive: new THREE.Color(def.accent),
      emissiveIntensity: 0.02,
    }),
  )
  const mesh = new THREE.Mesh(geometry, material)
  mesh.userData.planetId = def.id

  const atmoColor = new THREE.Color(
    def.id === 'earth' ? 0x4db8ff : def.id === 'mars' ? 0xff7040 : def.id === 'venus' ? 0xffc070 : 0xaaccff,
  )
  const atmosphere = new THREE.Mesh(
    track(new THREE.SphereGeometry(def.radius * 1.06, 64, 64)),
    createAtmosphereMaterial(atmoColor, def.id === 'earth' ? 0.45 : 0.28),
  )

  let clouds: THREE.Mesh | undefined
  if (def.clouds) {
    clouds = new THREE.Mesh(
      track(new THREE.SphereGeometry(def.radius * 1.025, 64, 64)),
      track(
        new THREE.MeshStandardMaterial({
          map: createCloudTexture(),
          transparent: true,
          opacity: 0.55,
          depthWrite: false,
        }),
      ),
    )
  }

  if (def.ring) {
    const ringGeom = track(new THREE.RingGeometry(def.radius * 1.45, def.radius * 2.35, 128))
    const ringMat = track(
      new THREE.MeshBasicMaterial({
        map: createRingTexture(),
        transparent: true,
        side: THREE.DoubleSide,
        depthWrite: false,
        opacity: 0.92,
      }),
    )
    const ring = new THREE.Mesh(ringGeom, ringMat)
    ring.rotation.x = Math.PI / 2.35
    mesh.add(ring)
  }

  return { mesh, atmosphere, clouds }
}

function createSun() {
  const group = new THREE.Group()
  const core = new THREE.Mesh(
    track(new THREE.SphereGeometry(2.2, 64, 64)),
    track(new THREE.MeshBasicMaterial({ color: 0xfff0c8 })),
  )
  const glow1 = new THREE.Mesh(
    track(new THREE.SphereGeometry(2.8, 32, 32)),
    track(new THREE.MeshBasicMaterial({ color: 0xffa040, transparent: true, opacity: 0.25, blending: THREE.AdditiveBlending, depthWrite: false })),
  )
  const glow2 = new THREE.Mesh(
    track(new THREE.SphereGeometry(4.2, 32, 32)),
    track(new THREE.MeshBasicMaterial({ color: 0xff6020, transparent: true, opacity: 0.08, blending: THREE.AdditiveBlending, depthWrite: false })),
  )
  group.add(core, glow1, glow2)
  return group
}

function waitForSize(el: HTMLElement, tries = 30): Promise<boolean> {
  return new Promise((resolve) => {
    const check = (left: number) => {
      if (el.clientWidth > 0 && el.clientHeight > 0) {
        resolve(true)
        return
      }
      if (left <= 0) {
        resolve(false)
        return
      }
      requestAnimationFrame(() => check(left - 1))
    }
    check(tries)
  })
}

function initScene() {
  const el = host.value
  if (!el || disposed || el.clientWidth <= 0 || el.clientHeight <= 0) {
    return
  }

  const planets: PlanetBody[] = []
  let selectedId: string | null = null
  const cameraTarget = new THREE.Vector3(8.5, 2.2, 18)
  const cameraLook = new THREE.Vector3(4, 0, 0)
  const desiredTarget = cameraTarget.clone()
  const desiredLook = cameraLook.clone()
  const defaultTarget = cameraTarget.clone()
  const defaultLook = cameraLook.clone()

  try {
    scene = new THREE.Scene()
    scene.background = createNebulaTexture()
    scene.fog = new THREE.FogExp2(0x03050c, 0.008)

    camera = new THREE.PerspectiveCamera(38, el.clientWidth / el.clientHeight, 0.1, 600)
    camera.position.copy(cameraTarget)

    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, powerPreference: 'high-performance' })
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2))
    renderer.setSize(el.clientWidth, el.clientHeight)
    renderer.toneMapping = THREE.ACESFilmicToneMapping
    renderer.toneMappingExposure = 1.15
    renderer.outputColorSpace = THREE.SRGBColorSpace
    el.appendChild(renderer.domElement)
    useCanvas.value = true
  } catch {
    disposeScene(false)
    useCanvas.value = false
    return
  }

  scene.add(new THREE.AmbientLight(0x1a2240, 0.25))
  const sunLight = new THREE.PointLight(0xffe8c0, 800, 150, 1.4)
  sunLight.position.set(0, 0, 0)
  scene.add(sunLight)
  const fill = new THREE.DirectionalLight(0x6080c0, 0.2)
  fill.position.set(-12, 6, -8)
  scene.add(fill)
  const rim = new THREE.DirectionalLight(0x40a0ff, 0.15)
  rim.position.set(8, 2, 10)
  scene.add(rim)

  const sun = createSun()
  scene.add(sun)
  scene.add(createStars())

  PLANETS.forEach((def) => {
    scene?.add(createOrbit(def.distance))
    const { mesh, atmosphere, clouds } = createPlanet(def)
    const pivot = new THREE.Object3D()
    pivot.add(mesh)
    pivot.add(atmosphere)
    if (clouds) {
      pivot.add(clouds)
    }
    scene?.add(pivot)
    planets.push({
      def,
      pivot,
      mesh,
      atmosphere,
      clouds,
      distance: def.distance,
      orbitSpeed: def.orbitSpeed,
      spinSpeed: def.spinSpeed,
      angle: Math.random() * Math.PI * 2,
      pulse: 0,
    })
  })

  const pickables = planets.map((p) => p.mesh)

  const focusPlanet = (body: PlanetBody | null) => {
    if (!body) {
      selectedId = null
      desiredTarget.copy(defaultTarget)
      desiredLook.copy(defaultLook)
      emit('select', null)
      return
    }
    selectedId = body.def.id
    body.pulse = 1
    const world = new THREE.Vector3()
    body.mesh.getWorldPosition(world)
    const dir = world.clone().normalize()
    desiredLook.copy(world)
    desiredTarget.copy(
      world.clone().add(dir.multiplyScalar(body.def.radius * 4.5 + 3.5)).add(new THREE.Vector3(0, body.def.radius * 0.7, 0)),
    )
    emit('select', {
      id: body.def.id,
      name: body.def.name,
      nameEn: body.def.nameEn,
      blurb: body.def.blurb,
      color: body.def.accent,
    })
  }

  const mars = planets.find((p) => p.def.id === 'mars')
  if (mars) {
    mars.angle = -0.55
    mars.pivot.position.set(
      Math.cos(mars.angle) * mars.distance,
      0,
      Math.sin(mars.angle) * mars.distance * 0.9,
    )
    const world = new THREE.Vector3()
    mars.mesh.getWorldPosition(world)
    cameraLook.copy(world)
    desiredLook.copy(world)
    cameraTarget.set(world.x + 5.2, world.y + 1.2, world.z + 7.5)
    desiredTarget.copy(cameraTarget)
    defaultTarget.copy(cameraTarget)
    defaultLook.copy(cameraLook)
  }

  clickHandler = (event: MouseEvent) => {
    if (!camera || !renderer) {
      return
    }
    const rect = renderer.domElement.getBoundingClientRect()
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(pointer, camera)
    const hits = raycaster.intersectObjects(pickables, false)
    if (!hits.length) {
      focusPlanet(null)
      return
    }
    const id = hits[0].object.userData.planetId as string
    focusPlanet(planets.find((p) => p.def.id === id) || null)
  }

  pointerHandler = (event: PointerEvent) => {
    if (!renderer || !camera) {
      return
    }
    const rect = renderer.domElement.getBoundingClientRect()
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(pointer, camera)
    renderer.domElement.style.cursor = raycaster.intersectObjects(pickables, false).length ? 'pointer' : 'default'
  }

  renderer.domElement.addEventListener('click', clickHandler)
  renderer.domElement.addEventListener('pointermove', pointerHandler)

  const onResize = () => {
    if (!camera || !renderer || !host.value) {
      return
    }
    const width = host.value.clientWidth
    const height = host.value.clientHeight
    if (width <= 0 || height <= 0) {
      return
    }
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
  }
  resizeObserver = new ResizeObserver(onResize)
  resizeObserver.observe(el)

  const clock = new THREE.Clock()
  const reduced = prefersReducedMotion()

  const tick = () => {
    if (disposed || !renderer || !scene || !camera) {
      return
    }
    const dt = Math.min(clock.getDelta(), 0.05)
    const t = clock.elapsedTime

    sun.rotation.y += 0.0008
    sun.children[1].scale.setScalar(1 + Math.sin(t * 1.2) * 0.04)
    sun.children[2].scale.setScalar(1 + Math.sin(t * 0.8) * 0.06)

    planets.forEach((planet) => {
      if (!reduced && selectedId !== planet.def.id) {
        planet.angle += planet.orbitSpeed * 0.0025
      } else if (!reduced && selectedId === planet.def.id) {
        planet.angle += planet.orbitSpeed * 0.0005
      }
      const y = reduced ? 0 : Math.sin(t * 0.15 + planet.distance) * 0.08
      planet.pivot.position.set(
        Math.cos(planet.angle) * planet.distance,
        y,
        Math.sin(planet.angle) * planet.distance * 0.9,
      )
      planet.mesh.rotation.y += planet.spinSpeed * (reduced ? 0.2 : 1)
      if (planet.clouds) {
        planet.clouds.rotation.y += planet.spinSpeed * 0.6
      }
      const mat = planet.mesh.material as THREE.MeshPhysicalMaterial
      if (planet.pulse > 0) {
        planet.pulse = Math.max(0, planet.pulse - dt * 1.2)
        mat.emissiveIntensity = 0.02 + planet.pulse * 0.6
        planet.atmosphere.scale.setScalar(1 + planet.pulse * 0.1)
      } else {
        mat.emissiveIntensity = selectedId === planet.def.id ? 0.1 : 0.02
        planet.atmosphere.scale.setScalar(selectedId === planet.def.id ? 1.05 : 1)
      }
    })

    if (selectedId) {
      const body = planets.find((p) => p.def.id === selectedId)
      if (body) {
        const world = new THREE.Vector3()
        body.mesh.getWorldPosition(world)
        const outward = world.clone().normalize()
        desiredLook.lerp(world, 0.1)
        desiredTarget.lerp(
          world.clone().add(outward.multiplyScalar(body.def.radius * 4.5 + 3.5)).add(new THREE.Vector3(0, body.def.radius * 0.65, 0)),
          0.07,
        )
      }
    }

    cameraTarget.lerp(desiredTarget, reduced ? 1 : 0.04)
    cameraLook.lerp(desiredLook, reduced ? 1 : 0.055)
    camera.position.copy(cameraTarget)
    camera.lookAt(cameraLook)

    renderer.render(scene, camera)
    animationId = requestAnimationFrame(tick)
  }
  tick()
}

function disposeScene(markDisposed = true) {
  if (markDisposed) {
    disposed = true
  }
  cancelAnimationFrame(animationId)
  resizeObserver?.disconnect()
  if (renderer && clickHandler) {
    renderer.domElement.removeEventListener('click', clickHandler)
  }
  if (renderer && pointerHandler) {
    renderer.domElement.removeEventListener('pointermove', pointerHandler)
  }
  disposables.forEach((item) => item.dispose())
  disposables.length = 0
  renderer?.dispose()
  renderer?.domElement.remove()
  renderer = null
  scene = null
  camera = null
}

onMounted(async () => {
  await nextTick()
  const el = host.value
  if (!el) {
    return
  }
  const ready = await waitForSize(el)
  if (!ready || disposed) {
    useCanvas.value = false
    return
  }
  initScene()
})

onUnmounted(() => {
  disposeScene()
})
</script>

<template>
  <div class="solar-root" aria-hidden="true">
    <div class="solar-fallback" />
    <div ref="host" class="solar-canvas" :class="{ active: useCanvas }" />
    <div class="vignette" />
  </div>
</template>

<style scoped>
.solar-root,
.solar-canvas,
.solar-fallback,
.vignette {
  position: absolute;
  inset: 0;
}

.solar-canvas {
  opacity: 0;
  pointer-events: auto;
}

.solar-canvas.active {
  opacity: 1;
}

.solar-canvas :deep(canvas) {
  display: block;
  width: 100% !important;
  height: 100% !important;
}

.solar-fallback {
  pointer-events: none;
  background:
    radial-gradient(ellipse at 70% 50%, rgba(180, 60, 30, 0.45) 0%, transparent 45%),
    radial-gradient(ellipse at 30% 20%, rgba(60, 100, 200, 0.15) 0%, transparent 50%),
    #03050c;
}

.vignette {
  pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 40%, rgba(3, 5, 12, 0.65) 100%);
}
</style>
