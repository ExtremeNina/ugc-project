# 项目设计参考表

## 1. 色彩系统

### 1.1 明亮主题（/css/style.css）

#### 主色体系
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --primary-color | #ff69b4 | 主色：粉色，核心识别色 |
| --primary-light | #ff85c0 | 主色浅：hover/高亮辅助 |
| --primary-dark | #e53e7a | 主色深：active/强调色 |

#### 辅助色体系
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --secondary-color | #87ceeb | 辅助色主色：天蓝色，交互提示 |
| --secondary-light | #b0e2ff | 辅助色浅：hover辅助 |

#### 强调色
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --accent-color | #ffd700 | 强调色：金色，用于重要突出 |

#### 中性色体系
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --bg-primary | #f9f7ff | 页面背景：淡紫色调 |
| --bg-secondary | #fff | 卡片/模块背景：白色 |
| --text-primary | #333 | 主文字：深灰色，标题、正文 |
| --text-secondary | #666 | 次要文字：中灰色，辅助信息 |

### 1.2 深色主题（/src/assets/main.css）

#### 主色体系 - 萌系科技紫
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --color-primary | #B892FF | 主色：柔和浅紫，核心识别色 |
| --color-primary-light | #D4C2FF | 主色浅：hover/高亮辅助 |
| --color-primary-dark | #976EFF | 主色深：active/强调色 |
| --color-primary-transparent-10 | rgba(184, 146, 255, 0.1) | 主色透明10%：蒙版/阴影用 |
| --color-primary-transparent-12 | rgba(184, 146, 255, 0.12) | 主色透明12%：tag hover阴影用 |
| --color-primary-transparent-15 | rgba(184, 146, 255, 0.15) | 主色透明15%：阴影用 |
| --color-primary-transparent-18 | rgba(184, 146, 255, 0.18) | 主色透明18%：卡片hover阴影用 |
| --color-primary-transparent-20 | rgba(184, 146, 255, 0.2) | 主色透明20%：蒙版/阴影用 |
| --color-primary-transparent-30 | rgba(184, 146, 255, 0.3) | 主色透明30%：蒙版/阴影用 |

#### 辅助色体系 - 清新科技青
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --color-secondary | #87E8DE | 辅助色主色：交互提示/次要强调 |
| --color-secondary-light | #B3F5ED | 辅助色浅：hover辅助 |
| --color-secondary-transparent-10 | rgba(135, 232, 222, 0.1) | 辅助色透明10%：点缀用 |
| --color-secondary-transparent-20 | rgba(135, 232, 222, 0.2) | 辅助色透明20%：点缀用 |

#### 中性色体系 - 深色模式层级
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --color-bg-page | #1E293B | 页面背景：深灰蓝 |
| --color-bg-card | #273449 | 卡片/模块背景：浅一级深灰 |
| --color-bg-card-hover | #2C3A52 | 卡片hover背景色 |
| --color-bg-divider | #1A2332 | 模块分隔背景：深一级深灰，用于导航栏等 |
| --color-bg-primary | var(--color-bg-page) | 兼容旧变量名 |
| --color-bg-secondary | var(--color-bg-card) | 兼容旧变量名 |
| --color-bg-tertiary | var(--color-bg-divider) | 兼容旧变量名 |

#### 文字颜色
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --color-text-primary | #F8FAFC | 主文字：浅灰白，标题、正文核心内容 |
| --color-text-secondary | #94A3B8 | 次要文字：淡灰蓝，发布时间、阅读量等 |
| --color-text-hint | #CBD5E1 | 提示文字：浅灰，辅助说明、占位符 |
| --text-primary | var(--color-text-primary) | 兼容旧变量名 |
| --text-secondary | var(--color-text-secondary) | 兼容旧变量名 |
| --text-tertiary | var(--color-text-hint) | 兼容旧变量名 |

#### 强调色 & 功能色
| 变量名 | 色值 | 描述 |
| --- | --- | --- |
| --accent-primary | var(--color-primary) | 主强调色 |
| --accent-primary-glow | var(--color-primary-transparent-30) | 主强调色光晕 |
| --accent-primary-gradient | linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%) | 主强调色渐变 |
| --accent-secondary | var(--color-secondary) | 次强调色 |
| --accent-secondary-glow | var(--color-secondary-transparent-20) | 次强调色光晕 |
| --accent-secondary-gradient | linear-gradient(135deg, var(--color-secondary) 0%, var(--color-secondary-light) 100%) | 次强调色渐变 |
| --accent-success | #66BB6A | 成功/安全色，柔和的绿色 |
| --accent-success-glow | rgba(102, 187, 106, 0.3) | 成功色光晕 |
| --accent-error | #EF5350 | 警告/错误色，低饱和度的红色 |
| --accent-error-glow | rgba(239, 83, 80, 0.3) | 错误色光晕 |
| --accent-info | #42A5F5 | 信息色，柔和的蓝色 |
| --accent-info-glow | rgba(66, 165, 245, 0.3) | 信息色光晕 |
| --accent-hot-rank | #87E8DE | 热门排名：浅青 |
| --accent-hot-rank-glow | rgba(135, 232, 222, 0.3) | 热门排名光晕 |

## 2. 排版系统

### 2.1 明亮主题（/css/style.css）

#### 字体
| 字体族 | 描述 |
| --- | --- |
| 'ZCOOL QingKe HuangYou', 'Microsoft YaHei', sans-serif | 主要字体：手写风格，适合二次元主题 |
| 'Ma Shan Zheng', cursive | Logo字体：毛笔风格 |
| 'ZCOOL KuaiLe', cursive | 辅助字体：快乐风格 |

#### 行高
| 行高值 | 描述 |
| --- | --- |
| 1.6 | 全局默认行高 |

### 2.2 深色主题（/src/assets/main.css）

#### 字体
| 字体族 | 描述 |
| --- | --- |
| 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif | 主要字体：现代无衬线字体 |
| 'Fira Code', 'Consolas', 'Monaco', monospace | 代码字体：等宽字体 |

#### 行高
| 行高值 | 描述 |
| --- | --- |
| 1.7 | 全局默认行高 |

## 3. 间距系统

### 3.1 明亮主题（/css/style.css）

| 间距值 | 描述 |
| --- | --- |
| 20px | 容器默认内边距 |
| 15px | 导航栏内边距 |
| 30px | 导航菜单项间距 |
| 15px | 用户操作区间距 |
| 10px | Logo元素间距 |
| 8px 15px | 导航链接内边距 |
| 8px 16px | 按钮内边距 |

### 3.2 深色主题（/src/assets/main.css）

| 变量名 | 间距值 | 描述 |
| --- | --- | --- |
| --spacing-xs | 4px | 极小间距 |
| --spacing-sm | 8px | 小间距 |
| --spacing-md | 16px | 中等间距 |
| --spacing-lg | 24px | 大间距 |
| --spacing-xl | 32px | 特大间距 |
| --spacing-xxl | 48px | 超大间距 |

## 4. 边框和圆角

### 4.1 明亮主题（/css/style.css）

| 变量名 | 值 | 描述 |
| --- | --- | --- |
| --border-radius | 12px | 全局圆角 |
| 20px | 导航链接/搜索框圆角 |
| 2px | 导航栏底部边框宽度 |
| 2px | 搜索框边框宽度 |

### 4.2 深色主题（/src/assets/main.css）

| 变量名 | 值 | 描述 |
| --- | --- | --- |
| --border-color | rgba(184, 146, 255, 0.1) | 默认边框色 |
| --border-color-light-primary | rgba(184, 146, 255, 0.5) | 浅紫色细微边框 |
| --border-color-light-secondary | rgba(135, 232, 222, 0.5) | 浅青色细微边框 |
| --border-gradient-primary | linear-gradient(135deg, var(--border-color) 0%, var(--border-color-light-primary) 100%) | 主色边框渐变 |
| --border-gradient-secondary | linear-gradient(135deg, var(--border-color) 0%, var(--border-color-light-secondary) 100%) | 辅助色边框渐变 |
| --border-radius | 10px | 默认圆角 |
| --border-radius-large | 14px | 大圆角 |
| --border-radius-full | 999px | 完全圆角 |

## 5. 阴影效果

### 5.1 明亮主题（/css/style.css）

| 变量名 | 阴影值 | 描述 |
| --- | --- | --- |
| --shadow | 0 4px 15px rgba(255, 105, 180, 0.1) | 全局默认阴影 |
| --shadow-hover | 0 8px 25px rgba(255, 105, 180, 0.2) | 悬停阴影 |

### 5.2 深色主题（/src/assets/main.css）

| 变量名 | 阴影值 | 描述 |
| --- | --- | --- |
| --shadow-sm | 0 2px 8px var(--color-primary-transparent-10) | 柔和主色阴影 |
| --shadow-md | 0 4px 12px var(--color-primary-transparent-15) | 中等主色阴影 |
| --shadow-lg | 0 6px 18px var(--color-primary-transparent-20) | 大主色阴影 |
| --shadow-card | 0 4px 12px var(--color-primary-transparent-10), 0 0 0 1px var(--border-color) | 卡片阴影 |
| --shadow-card-hover | 0 6px 18px var(--color-primary-transparent-18), 0 0 0 1px var(--border-color-light-primary) | 卡片悬停阴影 |
| --shadow-inset | inset 0 1px 3px rgba(0, 0, 0, 0.2), inset 0 0 3px var(--color-primary-transparent-10) | 内阴影 |
| --shadow-navbar | 0 2px 8px rgba(184,146,255,0.1) | 导航栏底部阴影 |
| --shadow-tag-hover | 0 2px 6px var(--color-primary-transparent-12) | 标签悬停阴影 |
| --shadow-banner | 0 4px 16px var(--color-primary-transparent-15) | Banner底部阴影 |
| --shadow-button-hover | 0 2px 8px var(--color-primary-transparent-20) | 按钮hover阴影 |

## 6. 过渡动画

### 6.1 明亮主题（/css/style.css）

| 过渡效果 | 描述 |
| --- | --- |
| all 0.3s ease | Logo悬停效果 |
| all 0.3s ease | 导航链接悬停效果 |
| left 0.5s | 导航链接背景滑动效果 |
| all 0.3s ease | 搜索框焦点效果 |
| all 0.3s ease | 搜索按钮悬停效果 |

### 6.2 深色主题（/src/assets/main.css）

| 变量名 | 过渡值 | 描述 |
| --- | --- | --- |
| --transition-fast | 0.2s ease | 快速过渡 |
| --transition-normal | 0.3s ease | 正常过渡 |
| --transition-slow | 0.5s ease | 慢速过渡 |

## 7. 背景渐变

### 7.1 明亮主题（/css/style.css）

| 渐变效果 | 描述 |
| --- | --- |
| linear-gradient(135deg, #fff, #f9f7ff) | 导航栏背景渐变 |
| linear-gradient(45deg, var(--primary-color), var(--secondary-color)) | Logo文字渐变 |
| radial-gradient(circle at 10% 20%, rgba(255, 105, 180, 0.05) 0%, transparent 20%) | 页面背景装饰 |
| radial-gradient(circle at 90% 80%, rgba(135, 206, 235, 0.05) 0%, transparent 20%) | 页面背景装饰 |

### 7.2 深色主题（/src/assets/main.css）

| 变量名 | 渐变值 | 描述 |
| --- | --- | --- |
| --bg-gradient-1 | linear-gradient(135deg, var(--color-bg-page) 0%, var(--color-bg-card) 100%) | 页面背景渐变 |
| --bg-gradient-2 | linear-gradient(135deg, var(--color-bg-card) 0%, var(--color-bg-page) 100%) | 反向页面背景渐变 |
| --bg-gradient-card | linear-gradient(135deg, var(--color-bg-card) 0%, rgba(39, 52, 73, 0.95) 100%) | 卡片背景渐变 |
| --banner-mask-gradient | linear-gradient(rgba(26,35,50,0.6), rgba(26,35,50,0.3)) | Banner图片蒙版渐变 |

## 8. 特殊效果

### 8.1 明亮主题（/css/style.css）

| 效果 | 描述 |
| --- | --- |
| backdrop-filter: blur(10px) | 导航栏毛玻璃效果 |
| text-shadow: 2px 2px 0px rgba(255, 105, 180, 0.3) | Logo文字阴影 |
| -webkit-background-clip: text | Logo文字背景裁剪 |
| -webkit-text-fill-color: transparent | Logo文字透明填充 |

### 8.2 深色主题（/src/assets/main.css）

| 效果 | 描述 |
| --- | --- |
| backdrop-filter: blur(10px) | 导航栏毛玻璃效果 |
| -webkit-font-smoothing: antialiased | 字体平滑 |
| -moz-osx-font-smoothing: grayscale | 字体平滑（macOS） |

## 9. 响应式设计

### 9.1 明亮主题（/css/style.css）

响应式断点主要针对移动端，具体断点值未在查看的代码中明确提及，但包含了移动端适配的媒体查询。

### 9.2 深色主题（/src/assets/main.css）

| 断点 | 描述 |
| --- | --- |
| max-width: 768px | 平板/移动端断点 |

## 10. 组件样式

### 10.1 明亮主题（/css/style.css）

#### 按钮样式
| 类名 | 描述 |
| --- | --- |
| .anime-button | 主要按钮样式，包含hover效果 |

#### 卡片样式
| 类名 | 描述 |
| --- | --- |
| .article-card | 文章卡片样式，包含阴影和hover效果 |

#### 导航样式
| 类名 | 描述 |
| --- | --- |
| .nav-header | 导航栏样式 |
| .nav-list | 导航列表样式 |
| .nav-item | 导航项样式 |
| .nav-link | 导航链接样式 |
| .nav-active | 激活导航项样式 |

### 10.2 深色主题（/src/assets/main.css）

#### 按钮样式
| 类名 | 描述 |
| --- | --- |
| .btn-primary | 主要按钮样式 |
| .btn-secondary | 次要按钮样式 |

#### 卡片样式
| 类名 | 描述 |
| --- | --- |
| .card | 基础卡片样式 |
| .card-hover | 卡片悬停效果 |

#### 导航样式
| 类名 | 描述 |
| --- | --- |
| .nav-header | 导航栏样式 |
| .nav-list | 导航列表样式 |
| .nav-item | 导航项样式 |
| .nav-link | 导航链接样式 |

## 11. 使用说明

### 11.1 明亮主题
适用于二次元风格的页面，以粉色为主色调，搭配天蓝色和金色作为辅助色，使用手写风格的字体，营造轻松活泼的氛围。

### 11.2 深色主题
适用于萌+科技风格的页面，以紫色和青色为主色调，使用现代无衬线字体，营造科技感和神秘感的氛围。

### 11.3 变量使用
在CSS中优先使用CSS变量，确保样式的一致性和可维护性。例如：

```css
.element {
  color: var(--text-primary);
  background-color: var(--bg-secondary);
  border-radius: var(--border-radius);
  padding: var(--spacing-md);
  transition: all var(--transition-normal);
}
```

### 11.4 主题切换
项目支持明亮和深色两种主题，可以通过切换样式文件或使用CSS变量覆盖实现主题切换。