export interface RoleVO {
  id: number
  code: string
  name: string
  remark?: string
  sort: number
  status: number
  builtin: number
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  status: number
  lastLoginAt?: string
  createdAt?: string
  roles: RoleVO[]
}

export interface MenuVO {
  id: number
  parentId: number
  name: string
  path?: string
  component?: string
  menuType: 'DIR' | 'MENU' | 'BUTTON'
  permission?: string
  icon?: string
  sort: number
  visible: number
  children: MenuVO[]
}

export interface LoginVO {
  token: string
  tokenType: string
  expiresIn: number
  user: UserVO
  menus: MenuVO[]
}

export interface CurrentUserVO {
  user: UserVO
  menus: MenuVO[]
  permissions: string[]
}

export interface PageResult<T> {
  total: number
  records: T[]
}
