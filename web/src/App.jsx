import React, { useEffect, useState } from 'react'
import { createI18n } from './i18n'

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080"
const i18n = createI18n('en')

export default function App() {
  const [lang, setLang] = useState(i18n.lang)
  const [showId, setShowId] = useState('got')
  const [targetSeason, setTargetSeason] = useState(4)
  const [immersion, setImmersion] = useState(2)
  const [items, setItems] = useState([])

  useEffect(()=> i18n.subscribe(setLang), [])

  async function fetchRecs(e){
    e.preventDefault()
    const res = await fetch(`${API_BASE}/recommendations`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({
        showId,
        targetSeason: Number(targetSeason),
        immersion: Number(immersion),
        locale: lang
      })
    })
    const data = await res.json()
    setItems(data.items || [])
  }

  const { t } = i18n

  return (
    <div style={{maxWidth: 720, margin: '40px auto', fontFamily:'Inter, system-ui, sans-serif'}}>
      <header style={{display:'flex', justifyContent:'space-between', alignItems:'center', marginBottom:16}}>
        <h1>{t('title')}</h1>
        <label style={{display:'flex', gap:8, alignItems:'center'}}>
          {t('lang')}
          <select value={lang} onChange={e=>{ i18n.setLang(e.target.value) }}>
            <option value="en">EN</option>
            <option value="ru">RU</option>
          </select>
        </label>
      </header>

      <form onSubmit={fetchRecs} style={{display:'grid', gap:12}}>
        <label>{t('showId')}
          <input value={showId} onChange={e=>setShowId(e.target.value)} />
        </label>
        <label>{t('targetSeason')}
          <input type="number" min="1" value={targetSeason} onChange={e=>setTargetSeason(e.target.value)} />
        </label>
        <label>{t('immersion')}
          <input type="number" min="1" max="5" value={immersion} onChange={e=>setImmersion(e.target.value)} />
        </label>
        <button type="submit">{t('getRecs')}</button>
      </form>

      <ul style={{marginTop:24}}>
        {items.map((it, idx)=>(
          <li key={idx}>
            <strong>{t('seasonEp', it.season, it.episode)}</strong>
            {it.title ? ` — ${it.title} — `: ' — '}<i>{it.reason}</i>
          </li>
        ))}
      </ul>
    </div>
  )
}