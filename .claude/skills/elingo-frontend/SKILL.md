---
name: elingo-frontend
description: REQUIRED for creating/editing any React component, page, route, CSS/style, or UI work in frontend/ of Elingo project — even without "convention" keyword. Uses React 19 + Vite 8, JavaScript (not TypeScript), custom router (not React Router), vanilla CSS (not Tailwind/MUI), Material Symbols Outlined icons, Axios via apiClient.js singleton, Liquid Glass Minimalism design system. Apply before touching any .jsx, .css, route definition, or frontend architecture.
---

# Elingo Frontend Skill

You are about to work on **Elingo frontend**. This skill ensures you follow the project's architecture, conventions, and design system.

## MANDATORY: Read Foundation Documents First

**Before creating or editing ANY UI file**, you MUST read:

1. **`frontend/docs/FE_CONVENTIONS.md`** — Stack, folder structure, naming, routing, API patterns, state management, form validation
2. **`frontend/docs/FE_DESIGN_SYSTEM.md`** — Visual system (Liquid Glass Minimalism): colors, typography, spacing, glass classes, buttons, cards, icons, dark mode

These two files apply to **EVERY frontend task** without exception.

---

## Progressive Disclosure: Read Additional Docs Based on Task

| Task Type | Additional Document to Read |
|---|---|
| Adding/editing routes or creating new pages | `frontend/docs/FE_ROUTES.md` |
| Creating new component (deciding where to place, what to reuse) | `frontend/docs/FE_COMPONENT_GUIDELINES.md` |
| Only editing styles of existing component | FE_DESIGN_SYSTEM.md is sufficient |
| Writing form logic, state management, or API calls | See relevant sections in FE_CONVENTIONS.md |

---

## 10 Hard Rules — NEVER Break These

These are extracted from the "BẮT BUỘC" and "CẦN TRÁNH" sections of the four foundation documents. Even if you skip reading everything, **remember these constraints**:

1. **JavaScript only** — NO TypeScript (`.js`, `.jsx` only)
2. **Vanilla CSS only** — NO Tailwind, MUI, Bootstrap, CSS-in-JS
3. **Custom router in `App.jsx`** — NO React Router (no `<Routes>`, `<Route>`, `<Link>`, `useNavigate`)
4. **One Axios instance** — Use `src/services/apiClient.js` singleton, NEVER create additional Axios instances
5. **Material Symbols Outlined only** — NO heroicons, lucide, react-icons, or other icon libraries
6. **CSS variables for colors** — ALWAYS use `var(--color-primary)`, NEVER hardcode hex colors in CSS/JSX
7. **Ambient orbs required** — Glass effect depends on 5 ambient orbs at app level (already in `App.jsx`)
8. **Glass classes for surfaces** — Use `.glass-card`, `.glass-capsule`, `.glass-pill`, etc. NO solid backgrounds for cards/containers
9. **Navigation via `onNavigate` prop** — Components receive `onNavigate` function, NEVER navigate directly with `window.location` or Router
10. **Dark mode support required** — Every new surface, input, alert, hover state MUST have `[data-theme='dark']` CSS with proper contrast

---

## Before You Start — Quick Checklist

- [ ] Read `frontend/docs/FE_CONVENTIONS.md` and `frontend/docs/FE_DESIGN_SYSTEM.md`
- [ ] Read additional docs based on task type (routes/components/etc)
- [ ] Check existing code for similar patterns before creating new files
- [ ] Verify if a shared component already exists in `src/components/` before creating new
- [ ] Plan where to place new component: `src/components/` (shared, 2+ modules) or `src/modules/moduleName/components/` (single module only)

---

## After Implementation — Verification Checklist

- [ ] **Tested dark mode**: Light theme AND dark theme both work with proper contrast
- [ ] **Tested responsive**: Desktop, 600px (phone), 360px (narrow) — no horizontal scroll, no cut-off content
- [ ] **Ran dev server**: Tested actual feature in browser (not just build success)
- [ ] **No console errors**: Check browser console for errors/warnings
- [ ] **Follows design system**: Glass classes, CSS variables, Material icons, proper spacing, pill buttons
- [ ] **Matches conventions**: Naming (PascalCase component, kebab-case CSS classes), folder structure, API via `apiClient.js`, `onNavigate` prop

---

## Stack Summary (for Reference)

| Category | Technology |
|---|---|
| Framework | React 19 + Vite 8 |
| Language | JavaScript (`.js` / `.jsx`) |
| Styling | Vanilla CSS (separate `.css` files) |
| Routing | Custom router in `App.jsx` |
| State | `useState` + Context API (`AuthContext`, `ThemeContext`) |
| HTTP | Axios via `src/services/apiClient.js` singleton |
| Icons | Material Symbols Outlined (Google) |
| Linting | oxlint |
| Design System | Liquid Glass Minimalism (frosted glass surfaces, ambient pastel orbs, organic softness) |

---

## Common Mistakes to Avoid

Based on "CẦN TRÁNH" sections from foundation docs:

- ❌ Using React Router (`<Routes>`, `<Route>`, `<Link>`, `useNavigate`)
- ❌ Creating additional Axios instances (only use `apiClient.js`)
- ❌ Using icon libraries other than Material Symbols Outlined
- ❌ Hardcoding colors (must use CSS variables like `var(--color-primary)`)
- ❌ Creating solid background cards (must use glass classes with blur)
- ❌ Forgetting ambient orbs (glass effect won't work without them)
- ❌ Using TypeScript (project is JavaScript only)
- ❌ Using CSS frameworks (Tailwind, MUI, Bootstrap)
- ❌ Creating custom button styles (use `.btn-primary`, `.btn-secondary`, `.btn-icon`)
- ❌ Navigating directly in components (must use `onNavigate` prop)
- ❌ Testing only light mode (must test both light and dark themes)
- ❌ Testing only desktop (must test desktop, 600px, 360px viewports)
- ❌ Creating duplicate components (check if shared component already exists)
- ❌ Placing module-specific components in `src/components/` (they belong in module folder)
- ❌ Placing shared components in module folders (they belong in `src/components/`)

---

## Folder Structure Quick Reference

```
frontend/src/
├── App.jsx                    # Root component — custom router + all routes
├── App.css                    # App shell CSS
├── main.jsx                   # Entry point — mount React, wrap Providers
├── index.css                  # Design tokens (CSS variables) + reset + utility classes
│
├── assets/                    # Static images, icons
│
├── components/                # Shared UI components (2+ modules)
│   ├── Header/               # PascalCase folders
│   ├── Footer/
│   ├── Input/
│   ├── Modal/
│   ├── Pagination/
│   └── Filter/
│
├── context/                   # Global React Contexts
│   ├── AuthContext.jsx
│   └── ThemeContext.jsx
│
├── modules/                   # Feature modules
│   ├── home/pages/           # Landing page
│   ├── auth/                 # authApi.js, components/, pages/
│   ├── battle/               # battleApi.js, context/, components/, pages/
│   ├── community/            # postApi.js, components/, pages/
│   ├── learning/             # Nested sub-modules:
│   │   ├── dictation/        # dictationApi.js, components/, pages/
│   │   ├── flashcard/        # flashcardApi.js, components/, pages/
│   │   ├── lesson/           # lessonApi.js, components/, pages/
│   │   └── shadowing/        # shadowingApi.js, components/, pages/
│   ├── user/                 # profileApi.js, components/, pages/
│   ├── vocabulary/           # vocabularyApi.js, components/, pages/
│   ├── admin/                # adminApi.js, layout/, components/, pages/
│   └── ...other modules
│
├── services/
│   └── apiClient.js          # Axios instance singleton — ONLY instance allowed
│
└── utils/
    └── utils.js              # Pure utility functions
```

---

## End Note

This skill is your navigation guide. The four foundation documents in `frontend/docs/` are the source of truth. Always refer to them for detailed rules, patterns, and examples.

**After reading this skill**: Go read the required foundation docs, then implement directly without repeating these rules back to the user.
