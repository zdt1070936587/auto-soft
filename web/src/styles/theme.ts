import { theme } from 'ant-design-vue'
import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'
import { designTokens } from './tokens'

export const appTheme: ThemeConfig = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: designTokens.primary,
    colorInfo: designTokens.accent,
    colorSuccess: designTokens.success,
    colorWarning: designTokens.warning,
    colorError: designTokens.danger,
    colorBgBase: designTokens.bgBase,
    colorBgLayout: designTokens.bgBase,
    colorBgContainer: designTokens.bgSurface,
    colorBgElevated: designTokens.bgElevated,
    colorBorder: designTokens.border,
    colorBorderSecondary: designTokens.border,
    colorText: designTokens.textPrimary,
    colorTextSecondary: designTokens.textSecondary,
    colorTextTertiary: designTokens.textMuted,
    borderRadius: designTokens.radiusSm,
    borderRadiusLG: designTokens.radiusMd,
    fontFamily: designTokens.fontSans,
    fontSize: 14,
    controlHeight: 36,
    wireframe: false,
  },
  components: {
    Card: {
      colorBgContainer: designTokens.bgSurface,
      borderRadiusLG: designTokens.radiusMd,
    },
  },
}
