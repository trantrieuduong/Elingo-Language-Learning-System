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
    const switchBtn = screen.getByRole('button', { name: 'Switch language' })

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

      const btn = screen.getByRole('button', { name: tr('nav:darkMode') })
      expect(btn).toHaveTextContent('dark_mode')

      await user.click(btn)
      expect(mockToggleTheme).toHaveBeenCalledTimes(1)
    })

    it('theme dark: nút hiển thị "light mode"', () => {
      mockTheme(true)
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      expect(screen.getByRole('button', { name: tr('nav:lightMode') })).toHaveTextContent('light_mode')
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
    it('hiển thị nút login/signup, không có hamburger/chuông/user menu', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      expect(screen.getByRole('button', { name: tr('nav:login') })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: tr('nav:signup') })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:openMenu') })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:notifications') })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:userMenu') })).not.toBeInTheDocument()
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

    it('không hiển thị nav link khi không phải trang chủ', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/login" />)

      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      expect(within(nav).queryAllByRole('link')).toHaveLength(0)
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
  })

  // ─────────────────────────────────────────────────────────────
  // Landing nav ở "/" (scroll-spy + click cuộn tới section)
  // ─────────────────────────────────────────────────────────────
  describe('Landing navigation (chưa đăng nhập, ở "/")', () => {
    const SECTION_IDS = ['hero', 'features', 'learning', 'community', 'pricing']
    let tops
    let sections
    let rafCallback
    const originalRaf = window.requestAnimationFrame
    const originalCaf = window.cancelAnimationFrame

    const linkOf = (key) => screen.getByRole('link', { name: tr(`nav:${key}`) })

    const scrollAndFlush = () => {
      window.dispatchEvent(new Event('scroll'))
      act(() => rafCallback())
    }

    beforeEach(() => {
      // hero đang ở đầu trang, các section còn lại nằm dưới (> headerOffset 130)
      tops = { hero: 0, features: 500, learning: 900, community: 1300, pricing: 1700 }
      sections = SECTION_IDS.map((id) => {
        const el = document.createElement('section')
        el.id = id
        el.getBoundingClientRect = () => ({ top: tops[id] })
        el.scrollIntoView = vi.fn()
        document.body.appendChild(el)
        return el
      })

      // rAF không chạy ngay: lưu callback để test tự flush
      rafCallback = null
      window.requestAnimationFrame = vi.fn((cb) => {
        rafCallback = cb
        return 42
      })
      window.cancelAnimationFrame = vi.fn()
    })

    afterEach(() => {
      sections.forEach((el) => el.remove())
      window.requestAnimationFrame = originalRaf
      window.cancelAnimationFrame = originalCaf
    })

    it('hiển thị 5 link landing và mặc định active "home"', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      expect(within(nav).getAllByRole('link')).toHaveLength(5)
      expect(linkOf('home')).toHaveClass(ACTIVE_NAV)
      expect(linkOf('features')).not.toHaveClass(ACTIVE_NAV)
    })

    it('scroll tới section nào thì link tương ứng được active', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      tops.features = 50 // features đã chạm đỉnh (<= 130)
      scrollAndFlush()

      expect(linkOf('features')).toHaveClass(ACTIVE_NAV)
      expect(linkOf('home')).not.toHaveClass(ACTIVE_NAV)

      tops.learning = 10
      tops.community = 20
      scrollAndFlush()

      expect(linkOf('community')).toHaveClass(ACTIVE_NAV) // section cuối cùng thoả điều kiện
      expect(linkOf('features')).not.toHaveClass(ACTIVE_NAV)
    })

    it('scroll nhưng section không đổi thì giữ nguyên active', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      scrollAndFlush()
      expect(linkOf('home')).toHaveClass(ACTIVE_NAV)
    })

    it('nhiều sự kiện scroll liên tiếp chỉ đặt 1 requestAnimationFrame', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      window.dispatchEvent(new Event('scroll'))
      window.dispatchEvent(new Event('scroll'))
      window.dispatchEvent(new Event('scroll'))

      expect(window.requestAnimationFrame).toHaveBeenCalledTimes(1)
    })

    it('unmount khi còn frame đang chờ thì hủy bằng cancelAnimationFrame và gỡ listener', () => {
      const removeSpy = vi.spyOn(window, 'removeEventListener')
      const { unmount } = render(<Header onNavigate={mockNavigate} currentPath="/" />)

      window.dispatchEvent(new Event('scroll'))
      unmount()

      expect(window.cancelAnimationFrame).toHaveBeenCalledWith(42)
      expect(removeSpy.mock.calls.some(([type]) => type === 'scroll')).toBe(true)
      removeSpy.mockRestore()
    })

    it('click link landing: chặn điều hướng mặc định, cuộn mượt tới section và đổi active', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      await user.click(linkOf('pricing'))

      const pricing = sections.find((s) => s.id === 'pricing')
      expect(pricing.scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start' })
      expect(linkOf('pricing')).toHaveClass(ACTIVE_NAV)
      expect(linkOf('home')).not.toHaveClass(ACTIVE_NAV)
      expect(mockNavigate).not.toHaveBeenCalled()
    })

    it('click link đang active vẫn cuộn tới section', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      await user.click(linkOf('home'))

      const hero = sections.find((s) => s.id === 'hero')
      expect(hero.scrollIntoView).toHaveBeenCalledTimes(1)
      expect(linkOf('home')).toHaveClass(ACTIVE_NAV)
    })

    it('không lỗi khi section không tồn tại trong DOM', async () => {
      sections.forEach((el) => el.remove())
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/" />)

      expect(linkOf('home')).toHaveClass(ACTIVE_NAV) // fallback 'hero'

      await user.click(linkOf('features'))
      expect(linkOf('features')).toHaveClass(ACTIVE_NAV)
    })
  })

  // ─────────────────────────────────────────────────────────────
  // Đã đăng nhập
  // ─────────────────────────────────────────────────────────────
  describe('Đã đăng nhập', () => {
    beforeEach(() => {
      mockAuth({ user: MOCK_USER })
    })

    it('hiển thị nav chính (3 link + dropdown Học tập), chuông thông báo, user menu; ẩn login/signup', () => {
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      expect(within(nav).getAllByRole('link')).toHaveLength(3)
      expect(within(nav).getByRole('button', { name: tr('nav:learning') })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: tr('nav:notifications') })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: tr('nav:userMenu') })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:login') })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: tr('nav:signup') })).not.toBeInTheDocument()
    })

    it('click nav link gọi onNavigate với path tương ứng', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      await user.click(within(nav).getByRole('link', { name: tr('nav:battle') }))

      expect(mockNavigate).toHaveBeenCalledWith('/battle')
    })

    it('mở dropdown học tập khi click, click option điều hướng đúng path', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/battle" />)

      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      const learningBtn = within(nav).getByRole('button', { name: tr('nav:learning') })

      expect(learningBtn).toHaveAttribute('aria-expanded', 'false')
      expect(screen.queryByRole('link', { name: tr('nav:vocabulary') })).not.toBeInTheDocument()
      expect(screen.queryByRole('link', { name: tr('nav:lessons') })).not.toBeInTheDocument()

      // Click mở dropdown
      await user.click(learningBtn)
      expect(learningBtn).toHaveAttribute('aria-expanded', 'true')
      const vocabLink = screen.getByRole('link', { name: tr('nav:vocabulary') })
      const lessonsLink = screen.getByRole('link', { name: tr('nav:lessons') })
      expect(vocabLink).toBeInTheDocument()
      expect(lessonsLink).toBeInTheDocument()

      // Click "Từ vựng" điều hướng /decks và đóng menu
      await user.click(vocabLink)
      expect(mockNavigate).toHaveBeenCalledWith('/decks')
      expect(screen.queryByRole('link', { name: tr('nav:vocabulary') })).not.toBeInTheDocument()

      // Mở lại và click "Bài học"
      await user.click(learningBtn)
      await user.click(screen.getByRole('link', { name: tr('nav:lessons') }))
      expect(mockNavigate).toHaveBeenCalledWith('/learning')
    })

    it('active dropdown button khớp đúng path học tập và các sub-path', () => {
      const { rerender } = render(<Header onNavigate={mockNavigate} currentPath="/learning" />)
      const nav = screen.getByRole('navigation', { name: 'Main navigation' })
      const getLearningBtn = () => within(nav).getByRole('button', { name: tr('nav:learning') })

      expect(getLearningBtn()).toHaveClass(ACTIVE_NAV)
      expect(within(nav).getByRole('link', { name: tr('nav:battle') })).not.toHaveClass(ACTIVE_NAV)

      rerender(<Header onNavigate={mockNavigate} currentPath="/learning/lesson-1" />)
      expect(getLearningBtn()).toHaveClass(ACTIVE_NAV)

      rerender(<Header onNavigate={mockNavigate} currentPath="/decks" />)
      expect(getLearningBtn()).toHaveClass(ACTIVE_NAV)

      rerender(<Header onNavigate={mockNavigate} currentPath="/decks/oxford-3000" />)
      expect(getLearningBtn()).toHaveClass(ACTIVE_NAV)

      // "/learning-extra" KHÔNG được coi là sub-path của "/learning"
      rerender(<Header onNavigate={mockNavigate} currentPath="/learning-extra" />)
      expect(getLearningBtn()).not.toHaveClass(ACTIVE_NAV)
    })

    it('click chuông thông báo điều hướng tới /notification', async () => {
      const user = userEvent.setup()
      render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

      await user.click(screen.getByRole('button', { name: tr('nav:notifications') }))
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
        const trigger = screen.getByRole('button', { name: tr('nav:userMenu') })
        await user.click(trigger)
        return trigger
      }

      it('mở/đóng dropdown khi click avatar và cập nhật aria-expanded', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)
        const trigger = screen.getByRole('button', { name: tr('nav:userMenu') })

        expect(trigger).toHaveAttribute('aria-expanded', 'false')
        expect(screen.queryByText(MOCK_USER.email)).not.toBeInTheDocument()

        await user.click(trigger)
        expect(trigger).toHaveAttribute('aria-expanded', 'true')
        expect(screen.getByText(MOCK_USER.fullName)).toBeInTheDocument()
        expect(screen.getByText(MOCK_USER.email)).toBeInTheDocument()

        await user.click(trigger)
        expect(trigger).toHaveAttribute('aria-expanded', 'false')
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

      await user.click(screen.getByRole('button', { name: tr('nav:logout') }))

      await waitFor(() => {
        expect(mockLogout).toHaveBeenCalledTimes(1)
        expect(mockNavigate).toHaveBeenCalledWith('/login')
      })
    })

    it('logout vẫn chạy bình thường khi không truyền onNavigate', async () => {
      const user = userEvent.setup()
      render(<Header currentPath="/learning" />)

      await user.click(screen.getByRole('button', { name: tr('nav:logout') }))

      await waitFor(() => expect(mockLogout).toHaveBeenCalledTimes(1))
      await user.click(screen.getByRole('button', { name: tr('nav:notifications') })) // handleNav khi không có onNavigate
      expect(mockNavigate).not.toHaveBeenCalled()
    })

    describe('Mobile menu', () => {
      it('hamburger mở menu, đổi label/aria-expanded và đóng lại khi click lần nữa', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        expect(screen.queryByRole('navigation', { name: 'Mobile navigation' })).not.toBeInTheDocument()

        const openBtn = screen.getByRole('button', { name: tr('nav:openMenu') })
        expect(openBtn).toHaveAttribute('aria-expanded', 'false')
        await user.click(openBtn)

        const closeBtn = screen.getByRole('button', { name: tr('nav:closeMenu') })
        expect(closeBtn).toHaveAttribute('aria-expanded', 'true')
        expect(screen.getByRole('navigation', { name: 'Mobile navigation' })).toBeInTheDocument()

        await user.click(closeBtn)
        expect(screen.queryByRole('navigation', { name: 'Mobile navigation' })).not.toBeInTheDocument()
      })

      it('mobile nav có các link chính và dropdown Học tập mở rộng submenu, click thì điều hướng + đóng menu', async () => {
        const user = userEvent.setup()
        render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await user.click(screen.getByRole('button', { name: tr('nav:openMenu') }))
        const mobileNav = screen.getByRole('navigation', { name: 'Mobile navigation' })

        // Vì currentPath là /learning nên group Học tập tự mở, hiển thị 2 submenu link + 3 link chính = 5 link
        expect(within(mobileNav).getAllByRole('link')).toHaveLength(5)
        const learningBtn = within(mobileNav).getByRole('button', { name: tr('nav:learning') })
        expect(learningBtn).toHaveClass('header__mobile-nav-link--active')
        expect(within(mobileNav).getByRole('link', { name: tr('nav:lessons') })).toHaveClass('header__mobile-submenu-link--active')
        expect(within(mobileNav).getByRole('link', { name: tr('nav:battle') })).not.toHaveClass('header__mobile-nav-link--active')

        // Click submenu link "Từ vựng" điều hướng và đóng menu
        await user.click(within(mobileNav).getByRole('link', { name: tr('nav:vocabulary') }))
        expect(mockNavigate).toHaveBeenCalledWith('/decks')
        expect(screen.queryByRole('navigation', { name: 'Mobile navigation' })).not.toBeInTheDocument()
      })

      it('tự đóng mobile menu khi currentPath thay đổi', async () => {
        const user = userEvent.setup()
        const { rerender } = render(<Header onNavigate={mockNavigate} currentPath="/learning" />)

        await user.click(screen.getByRole('button', { name: tr('nav:openMenu') }))
        expect(screen.getByRole('navigation', { name: 'Mobile navigation' })).toBeInTheDocument()

        rerender(<Header onNavigate={mockNavigate} currentPath="/battle" />)
        expect(screen.queryByRole('navigation', { name: 'Mobile navigation' })).not.toBeInTheDocument()
      })
    })
  })
})
