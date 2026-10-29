import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, within, fireEvent, act, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Header from '@/components/Header/Header'
import { useAuth } from '@/context/AuthContext'
import { useTheme } from '@/context/ThemeContext'
import { MOCK_AUTH_CONTEXT_DEFAULT, MOCK_USER } from '../../utils/mockData'
import { tr } from '../../utils/i18n'

vi.mock('@/context/AuthContext')
vi.mock('@/context/ThemeContext')

const ACTIVE_NAV = 'header__nav-link--active'
const getDesktopNav = () => document.querySelector('.header__nav')
const getMobileNav = () => document.querySelector('.header__mobile-nav')
const getHeaderButton = (selector) => document.querySelector(selector)

describe('Header', () => {
  const mockLogout = vi.fn()
  const mockToggleTheme = vi.fn()
  const mockNavigate = vi.fn()

  const mockAuth = (overrides = {}) =>
    useAuth.mockReturnValue({ ...MOCK_AUTH_CONTEXT_DEFAULT, logout: mockLogout, ...overrides })

  const mockTheme = (isDark = false) =>
    useTheme.mockReturnValue({ isDark, toggleTheme: mockToggleTheme })

  beforeEach(() => {
    mockLogout.mockResolvedValue(undefined)
    mockAuth()
    mockTheme()
  })

  // ─────────────────────────────────────────────────────────────
  // Test gốc: giữ nguyên
  // ─────────────────────────────────────────────────────────────
  it('language switch: toggles vi <-> en, updates UI text, <html lang> and persisted choice', async () => {
    const user = userEvent.setup()
    render(<Header onNavigate={vi.fn()} currentPath="/" />)
    const switchBtn = getHeaderButton('.header__lang-btn')

    expect(switchBtn).toHaveTextContent('VI')
    expect(screen.getAllByText(tr('nav:features')).length).toBeGreaterThan(0)

    await user.click(switchBtn)

    expect(switchBtn).toHaveTextContent('EN')
    expect(document.documentElement.lang).toBe('en')
    expect(localStorage.getItem('elingo_lng')).toBe('en')
    expect(screen.getAllByText(tr('nav:features')).length).toBeGreaterThan(0) // tr() lúc này trả bản en

    await user.click(switchBtn)
    expect(switchBtn).toHaveTextContent('VI')
    expect(document.documentElement.lang).toBe('vi')
  })

  // ─────────────────────────────────────────────────────────────
  // Chung: brand, theme toggle, class theo path
  // ─────────────────────────────────────────────────────────────
  describe('Brand, theme toggle và class', () => {
    it('click brand điều hướng về "/"', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      await user.click(screen.getByRole('link', { name: 'Elingo' }))
      expect(mockNavigate).toHaveBeenCalledWith('/')
    })

    it('theme light: nút hiển thị "dark mode" và click gọi toggleTheme', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      const btn = screen.getByText('dark_mode').closest('button')
      expect(btn).toHaveTextContent('dark_mode')

      await user.click(btn)
      expect(mockToggleTheme).toHaveBeenCalledTimes(1)
    })

    it('theme dark: nút hiển thị "light mode"', () => {
      mockTheme(true)
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      expect(screen.getByText('light_mode').closest('button')).toHaveTextContent('light_mode')
    })

    it('thêm class header--auth khi không ở trang chủ', () => {
      const { container, rerender } = render(<Header onNavigate={mockNavigate} currentPath="/" />)
      expect(container.querySelector('header')).not.toHaveClass('header--auth')

      rerender(<Header onNavigate={mockNavigate} currentPath="/login" />)
      expect(container.querySelector('header')).toHaveClass('header--auth')
    })
  })

  // ─────────────────────────────────────────────────────────────
  // Chưa đăng nhập
  // ─────────────────────────────────────────────────────────────
  describe('Chưa đăng nhập', () => {
    it('hiển thị nút login/signup, có hamburger khi ở các trang ngoài landing page, không có chuông/user menu', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      expect(screen.getByRole('button', { name: tr('nav:login') })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: tr('nav:signup') })).toBeInTheDocument()
      expect(getHeaderButton('.header__hamburger')).toBeInTheDocument()
      expect(getHeaderButton('.header__avatar')).not.toBeInTheDocument()
    })

    it('click login / signup gọi onNavigate đúng path', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      await user.click(screen.getByRole('button', { name: tr('nav:login') }))
      expect(mockNavigate).toHaveBeenCalledWith('/login')

      await user.click(screen.getByRole('button', { name: tr('nav:signup') }))
      expect(mockNavigate).toHaveBeenCalledWith('/signup')
    })

    it('đánh dấu nút login/signup active theo currentPath', () => {
      const { rerender } = render(<Header onNavigate={mockNavigate} currentPath="/login" />)
      expect(screen.getByRole('button', { name: tr('nav:login') })).toHaveClass('btn-primary', 'header__auth-button--active')
      expect(screen.getByRole('button', { name: tr('nav:signup') })).toHaveClass('btn-secondary')

      rerender(<Header onNavigate={mockNavigate} currentPath="/signup" />)
      expect(screen.getByRole('button', { name: tr('nav:signup') })).toHaveClass('btn-primary', 'header__auth-button--active')
      expect(screen.getByRole('button', { name: tr('nav:login') })).toHaveClass('btn-secondary')
    })

    it('hiển thị đầy đủ nav link khi chưa đăng nhập ở trang auth (/login)', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      const nav = getDesktopNav()
      expect(within(nav).getByRole('link', { name: tr('nav:home') })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:features')) })).toBeInTheDocument()
    })

    it('không gắn scroll listener khi không ở trang chủ', () => {
      const addSpy = vi.spyOn(window, 'addEventListener')
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      expect(addSpy.mock.calls.some(([type]) => type === 'scroll')).toBe(false)
      addSpy.mockRestore()
    })

    it('không gọi onNavigate được khi prop không truyền (không crash)', async () => {
      const user = userEvent.setup()
      render(<Header currentPath="/" />)

      await user.click(screen.getByRole('button', { name: tr('nav:login') }))
      await user.click(screen.getByRole('link', { name: 'Elingo' }))
      // không có expect lỗi => không throw
      expect(screen.getByRole('button', { name: tr('nav:login') })).toBeInTheDocument()
    })

    it('ở trang chủ "/" khi chưa đăng nhập, nav vẫn đồng bộ và click home link dẫn về "/"', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      const nav = getDesktopNav()
      const homeLink = within(nav).getByRole('link', { name: tr('nav:home') })
      expect(homeLink).toBeInTheDocument()
      expect(homeLink).toHaveClass(ACTIVE_NAV)

      await user.click(homeLink)
      expect(mockNavigate).toHaveBeenCalledWith('/')
    })
  })

  // ─────────────────────────────────────────────────────────────
  // Đã đăng nhập
  // ─────────────────────────────────────────────────────────────
  describe('Đã đăng nhập', () => {
    beforeEach(() => {
      mockAuth({ user: MOCK_USER })
    })

    it('hiển thị 5 mục nav chính, chuông thông báo, user menu; ẩn login/signup', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      const nav = getDesktopNav()
      expect(within(nav).getByRole('link', { name: tr('nav:home') })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:features')) })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:community')) })).toBeInTheDocument()
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:pricing')) })).toBeInTheDocument()
      expect(getHeaderButton('.header__right .btn-icon:nth-of-type(3)')).toBeInTheDocument()
      expect(getHeaderButton('.header__avatar')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:login') })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:signup') })).not.toBeInTheDocument()
    })

    it('hover hoặc click dropdown nav item mở menu và click sub-item gọi onNavigate với path tương ứng', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/dashboard" />)

      const nav = getDesktopNav()
      const featuresBtn = within(nav).getByRole('button', { name: new RegExp(tr('nav:features')) })
      const navItem = featuresBtn.closest('.header__nav-item')
      fireEvent.mouseEnter(navItem)

      const speakingLink = await screen.findByRole('link', { name: new RegExp(tr('nav:speakingRoom')) })
      expect(speakingLink).toBeInTheDocument()
      await user.click(speakingLink)

      expect(mockNavigate).toHaveBeenCalledWith('/speaking')
    })

    it('active link khớp đúng path của dropdown sub-item và kích hoạt active trên parent button', () => {
      const { rerender } = render(<Header onNavigate={mockNavigate} currentPath="/lessons" />)
      const nav = getDesktopNav()

      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).toHaveClass(ACTIVE_NAV)
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:features')) })).not.toHaveClass(ACTIVE_NAV)

      rerender(<Header onNavigate={mockNavigate} currentPath="/decks" />)
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).toHaveClass(ACTIVE_NAV)

      rerender(<Header onNavigate={mockNavigate} currentPath="/dashboard" />)
      expect(within(nav).getByRole('link', { name: tr('nav:home') })).toHaveClass(ACTIVE_NAV)
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).not.toHaveClass(ACTIVE_NAV)

      // Kiểm tra route lồng nhau: /community/my-posts chỉ active mục "Bài viết của tôi", không active nhầm mục "Bảng tin" (/community)
      rerender(<Header onNavigate={mockNavigate} currentPath="/community/my-posts" />)
      expect(within(nav).getByRole('button', { name: new RegExp(tr('nav:community')) })).toHaveClass(ACTIVE_NAV)
      const communityBtn = within(nav).getByRole('button', { name: new RegExp(tr('nav:community')) })
      const communityNavItem = communityBtn.closest('.header__nav-item')
      fireEvent.mouseEnter(communityNavItem)

      const myPostsLink = screen.getByRole('link', { name: new RegExp(tr('nav:myPosts')) })
      const feedLink = screen.getByRole('link', { name: new RegExp(tr('nav:feed')) })
      expect(myPostsLink).toHaveClass('header__dropdown-item--active')
      expect(feedLink).not.toHaveClass('header__dropdown-item--active')
    })

    it('click chuông thông báo điều hướng tới /notification', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      await user.click(screen.getByText('notifications').closest('button'))
      expect(mockNavigate).toHaveBeenCalledWith('/notification')
    })

    it('không gắn scroll listener khi đã đăng nhập (kể cả ở "/")', () => {
      const addSpy = vi.spyOn(window, 'addEventListener')
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      expect(addSpy.mock.calls.some(([type]) => type === 'scroll')).toBe(false)
      addSpy.mockRestore()
    })

    describe('User dropdown', () => {
      const openDropdown = async (user) => {
        const trigger = getHeaderButton('.header__avatar')
        await user.click(trigger)
        return trigger
      }

      it('mở/đóng dropdown khi click avatar', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)
        const trigger = getHeaderButton('.header__avatar')

        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()

        await user.click(trigger)
        expect(screen.getByText(MOCK_USER.fullName)).toBeInTheDocument()
        expect(screen.getByText(MOCK_USER.email)).toBeInTheDocument()

        await user.click(trigger)
        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()
      })

      it('hiển thị email thay cho tên khi user không có fullName', async () => {
        mockAuth({ user: { ...MOCK_USER, fullName: undefined } })
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await openDropdown(user)
        // email xuất hiện 2 lần: dòng tên (fallback) + dòng email
        expect(screen.getAllByText(MOCK_USER.email)).toHaveLength(2)
      })

      it.each([
        ['profile', '/profile'],
        ['progress', '/progress'],
        ['premium', '/premium'],
      ])('click "%s" điều hướng tới %s và đóng dropdown', async (key, path) => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await openDropdown(user)
        await user.click(screen.getByText(tr(`nav:${key}`)))

        expect(mockNavigate).toHaveBeenCalledWith(path)
        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()
      })

      it('click ra ngoài thì đóng dropdown, click bên trong thì giữ nguyên', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await openDropdown(user)

        fireEvent.mouseDown(screen.getByText(MOCK_USER.email)) // bên trong
        expect(screen.getByText(MOCK_USER.email)).toBeInTheDocument()

        fireEvent.mouseDown(document.body) // bên ngoài
        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()
      })

      it('logout từ dropdown: đóng menu, gọi logout rồi điều hướng /login', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await openDropdown(user)
        await user.click(screen.getByText(tr('nav:logout')))

        await waitFor(() => {
          expect(mockLogout).toHaveBeenCalledTimes(1)
          expect(mockNavigate).toHaveBeenCalledWith('/login')
        })
        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()
      })
    })

    it('logout bằng nút icon trên header: gọi logout rồi điều hướng /login', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      await user.click(getHeaderButton('.header__logout-button'))

      await waitFor(() => {
        expect(mockLogout).toHaveBeenCalledTimes(1)
        expect(mockNavigate).toHaveBeenCalledWith('/login')
      })
    })

    it('logout vẫn chạy bình thường khi không truyền onNavigate', async () => {
      const user = userEvent.setup()
      render(<Header currentPath="/learning" />)

      await user.click(getHeaderButton('.header__logout-button'))

      await waitFor(() => expect(mockLogout).toHaveBeenCalledTimes(1))
      await user.click(screen.getByText('notifications').closest('button')) // handleNav khi không có onNavigate
      expect(mockNavigate).not.toHaveBeenCalled()
    })

    describe('Mobile menu', () => {
      it('hamburger mở và đóng menu khi click lần nữa', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        expect(getMobileNav()).not.toBeInTheDocument()

        const openBtn = getHeaderButton('.header__hamburger')
        await user.click(openBtn)

        const closeBtn = getHeaderButton('.header__hamburger')
        expect(getMobileNav()).toBeInTheDocument()

        await user.click(closeBtn)
        expect(getMobileNav()).not.toBeInTheDocument()
      })

      it('mobile nav có các mục và nhóm dropdown, đánh dấu active và click thì điều hướng + đóng menu', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/lessons" />)

        await user.click(getHeaderButton('.header__hamburger'))
        const mobileNav = getMobileNav()

        expect(within(mobileNav).getByRole('link', { name: tr('nav:home') })).toBeInTheDocument()
        expect(within(mobileNav).getByRole('button', { name: new RegExp(tr('nav:learning')) })).toHaveClass('header__mobile-nav-link--active')

        // Click home link -> navigates to /dashboard and closes menu
        await user.click(within(mobileNav).getByRole('link', { name: tr('nav:home') }))

        expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
        expect(getMobileNav()).not.toBeInTheDocument()
      })

      it('tự đóng mobile menu khi currentPath thay đổi', async () => {
        const user = userEvent.setup()
        const { rerender } = render(<Header onNavigate={mockNavigate} currentPath="/lessons" />)

        await user.click(getHeaderButton('.header__hamburger'))
        expect(getMobileNav()).toBeInTheDocument()

        rerender(<Header onNavigate={mockNavigate} currentPath="/battle" />)
        expect(getMobileNav()).not.toBeInTheDocument()
      })
    })
  })
})
