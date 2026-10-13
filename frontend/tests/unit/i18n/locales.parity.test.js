import { describe, it, expect } from 'vitest'

const files = import.meta.glob('/src/locales/*/*.json', { eager: true, import: 'default' })

const byLang = {}
for (const [path, json] of Object.entries(files)) {
  const [, lng, ns] = path.match(/locales\/(\w+)\/(\w+)\.json$/)
  ;(byLang[lng] ??= {})[ns] = json
}

const keys = (obj, prefix = '') =>
  Object.entries(obj).flatMap(([k, v]) =>
    v && typeof v === 'object' ? keys(v, `${prefix}${k}.`) : [`${prefix}${k}`],
  )

const namespaces = [...new Set(Object.values(byLang).flatMap(Object.keys))]

describe.each(namespaces)('locale "%s"', (ns) => {
  it('vi và en có cùng bộ key', () => {
    expect(keys(byLang.en?.[ns] ?? {}).sort()).toEqual(keys(byLang.vi?.[ns] ?? {}).sort())
  })
})
