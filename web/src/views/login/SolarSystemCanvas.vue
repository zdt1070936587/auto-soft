<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import * as THREE from 'three'

const host = ref<HTMLDivElement | null>(null)
const fallback = ref(false)

let renderer: THREE.WebGLRenderer | null = null
let scene: THREE.Scene | null = null
let camera: THREE.PerspectiveCamera | null = null
let animationId = 0
let disposed = false
let resizeHandler: (() => void) | null = null
const disposables: Array<THREE.BufferGeometry | THREE.Material | THREE.Texture> = []

interface PlanetDef {
  color: number
  radius: number
  distance: number
  orbitSpeed: number
  spinSpeed: number
  ring?: boolean
}

interface PlanetBody {
  mesh: THREE.Mesh
  distance: number
  orbitSpeed: number
  spinSpeed: number
  angle: number
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

function track<T extends THREE.BufferGeometry | THREE.Material | THREE.Texture>(item: T): T {
  disposables.push(item)
  return item
}

function createStars() {
  const count = 1400
  const positions = new Float32Array(count * 3)
  for (let i = 0; i < count; i += 1) {
    const radius = 80 + Math.random() * 140
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = radius * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = radius * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = radius * Math.cos(phi)
  }
  const geometry = track(new THREE.BufferGeometry())
  geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const material = track(
    new THREE.PointsMaterial({
      color: 0xdce7ff,
      size: 0.35,
      transparent: true,
      opacity: 0.85,
      sizeAttenuation: true,
    }),
  )
  return new THREE.Points(geometry, material)
}

function createOrbit(radius: number) {
  const curve = new THREE.EllipseCurve(0, 0, radius, radius * 0.92, 0, Math.PI * 2, false, 0)
  const points = curve.getPoints(128).map((p) => new THREE.Vector3(p.x, 0, p.y))
  const geometry = track(new THREE.BufferGeometry().setFromPoints(points))
  const material = track(
    new THREE.LineBasicMaterial({ color: 0x6b7aa8, transparent: true, opacity: 0.28 }),
  )
  return new THREE.LineLoop(geometry, material)
}

function createPlanet(color: number, radius: number) {
  const geometry = track(new THREE.SphereGeometry(radius, 32, 32))
  const material = track(
    new THREE.MeshStandardMaterial({
      color,
      roughness: 0.55,
      metalness: 0.08,
      emissive: color,
      emissiveIntensity: 0.08,
    }),
  )
  return new THREE.Mesh(geometry, material)
}

function initScene() {
  const el = host.value
  if (!el) {
    return
  }
  const canvas = document.createElement('canvas')
  const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
  if (!gl) {
    fallback.value = true
    return
  }

  scene = new THREE.Scene()
  scene.fog = new THREE.FogExp2(0x050814, 0.012)

  camera = new THREE.PerspectiveCamera(48, el.clientWidth / el.clientHeight, 0.1, 400)
  camera.position.set(0, 9.5, 26)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true, canvas })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
  renderer.setSize(el.clientWidth, el.clientHeight)
  renderer.setClearColor(0x050814, 1)
  el.appendChild(renderer.domElement)

  const ambient = new THREE.AmbientLight(0x6f7aa8, 0.45)
  scene.add(ambient)

  const sunLight = new THREE.PointLight(0xffe2a8, 180, 80, 1.6)
  scene.add(sunLight)

  const sunGeom = track(new THREE.SphereGeometry(1.8, 48, 48))
  const sunMat = track(
    new THREE.MeshBasicMaterial({ color: 0xffd27a }),
  )
  const sun = new THREE.Mesh(sunGeom, sunMat)
  scene.add(sun)

  const glowGeom = track(new THREE.SphereGeometry(2.35, 32, 32))
  const glowMat = track(
    new THREE.MeshBasicMaterial({
      color: 0xffb347,
      transparent: true,
      opacity: 0.18,
    }),
  )
  scene.add(new THREE.Mesh(glowGeom, glowMat))

  scene.add(createStars())

  const planetDefs: PlanetDef[] = [
    { color: 0xb1b1b1, radius: 0.18, distance: 4.2, orbitSpeed: 1.55, spinSpeed: 0.02 },
    { color: 0xe8c37a, radius: 0.32, distance: 5.8, orbitSpeed: 1.12, spinSpeed: 0.012 },
    { color: 0x3a7bd5, radius: 0.34, distance: 7.6, orbitSpeed: 0.92, spinSpeed: 0.03 },
    { color: 0xc1440e, radius: 0.24, distance: 9.3, orbitSpeed: 0.74, spinSpeed: 0.028 },
    { color: 0xd9b48f, radius: 0.82, distance: 12.4, orbitSpeed: 0.4, spinSpeed: 0.055 },
    { color: 0xe3d5a3, radius: 0.7, distance: 15.6, orbitSpeed: 0.3, spinSpeed: 0.05, ring: true },
    { color: 0x7fd3e0, radius: 0.46, distance: 18.4, orbitSpeed: 0.22, spinSpeed: 0.04 },
    { color: 0x4166f5, radius: 0.44, distance: 21.0, orbitSpeed: 0.16, spinSpeed: 0.036 },
  ]

  const planets: PlanetBody[] = []
  planetDefs.forEach((def) => {
    scene?.add(createOrbit(def.distance))
    const mesh = createPlanet(def.color, def.radius)
    if (def.ring) {
      const ringGeom = track(new THREE.RingGeometry(def.radius * 1.3, def.radius * 2.1, 48))
      const ringMat = track(
        new THREE.MeshBasicMaterial({
          color: 0xcbb48a,
          side: THREE.DoubleSide,
          transparent: true,
          opacity: 0.55,
        }),
      )
      const ring = new THREE.Mesh(ringGeom, ringMat)
      ring.rotation.x = Math.PI / 2.4
      mesh.add(ring)
    }
    scene?.add(mesh)
    planets.push({
      mesh,
      distance: def.distance,
      orbitSpeed: def.orbitSpeed,
      spinSpeed: def.spinSpeed,
      angle: Math.random() * Math.PI * 2,
    })
  })

  const clock = new THREE.Clock()
  const reduced = prefersReducedMotion()
  planets.forEach((planet) => {
    planet.mesh.position.set(Math.cos(planet.angle) * planet.distance, 0, Math.sin(planet.angle) * planet.distance * 0.92)
  })

  const onResize = () => {
    if (!camera || !renderer || !host.value) {
      return
    }
    const width = host.value.clientWidth
    const height = host.value.clientHeight
    camera.aspect = width / height
    camera.updateProjectionMatrix()
    renderer.setSize(width, height)
  }
  resizeHandler = onResize
  window.addEventListener('resize', onResize)

  const tick = () => {
    if (disposed || !renderer || !scene || !camera) {
      return
    }
    const t = clock.getElapsedTime()
    if (!reduced) {
      planets.forEach((planet) => {
        planet.angle += planet.orbitSpeed * 0.004
        planet.mesh.position.set(
          Math.cos(planet.angle) * planet.distance,
          Math.sin(t * 0.2 + planet.distance) * 0.12,
          Math.sin(planet.angle) * planet.distance * 0.92,
        )
        planet.mesh.rotation.y += planet.spinSpeed
      })
      camera.position.x = Math.sin(t * 0.05) * 1.6
      camera.position.y = 9.5 + Math.sin(t * 0.03) * 0.4
    }
    camera.lookAt(0, 0, 0)
    renderer.render(scene, camera)
    animationId = requestAnimationFrame(tick)
  }
  tick()
}

function disposeScene() {
  disposed = true
  cancelAnimationFrame(animationId)
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  disposables.forEach((item) => item.dispose())
  disposables.length = 0
  renderer?.dispose()
  renderer?.domElement.remove()
  renderer = null
  scene = null
  camera = null
}

onMounted(() => {
  initScene()
})

onUnmounted(() => {
  disposeScene()
})
</script>

<template>
  <div class="solar-root">
    <div v-show="!fallback" ref="host" class="solar-canvas" />
    <div v-if="fallback" class="solar-fallback" />
  </div>
</template>

<style scoped>
.solar-root,
.solar-canvas,
.solar-fallback {
  position: absolute;
  inset: 0;
}

.solar-fallback {
  background:
    radial-gradient(circle at 50% 48%, #ffd27a 0 2%, transparent 8%),
    radial-gradient(circle at 20% 30%, #fff 0 1px, transparent 2px),
    radial-gradient(circle at 70% 20%, #fff 0 1px, transparent 2px),
    radial-gradient(circle at 40% 70%, #fff 0 1px, transparent 2px),
    radial-gradient(circle at 85% 60%, #fff 0 1px, transparent 2px),
    radial-gradient(circle at 15% 80%, #9ab 0 1px, transparent 2px),
    linear-gradient(180deg, #050814, #0b1636 70%, #050814);
}
</style>
