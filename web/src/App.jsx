import React, { useEffect, useMemo, useState } from 'react'
import { createI18n } from './i18n'

const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080"

function ErrorBoundary({ children }) {
  const [err, setErr] = useState(null)
  if (err) {
    return (
      <div style={{maxWidth: 720, margin: '40px auto', fontFamily:'Inter, system-ui, sans-serif', color:'#b00020'}}>
        <h2>UI error</h2>
        <pre style={{whiteSpace:'pre-wrap'}}>{String(err?.stack || err)}</pre>
      </div>
    )
  }
  return (
    <React.ErrorBoundary fallbackRender={({error})=>{
      return (
        <div style={{maxWidth: 720, margin: '40px auto', fontFamily:'Inter, system-ui, sans-serif', color:'#b00020'}}>
          <h2>UI error</h2>
          <pre style={{whiteSpace:'pre-wrap'}}>{String(error?.stack || error)}</pre>
        </div>
      )
    }}>
      {React.cloneElement(children, { setOuterError: setErr })}
    </React.ErrorBoundary>
  )
}

export default function App({ setOuterError }) {
  // инициализируем i18n один раз
  const i18n = useMemo(()=>createI18n('en'), [])
  const [lang, setLang] = useState(i18n.lang)
  const [showId, setShowId] = useState('got')
  const [targetSeason, setTargetSeason] = useState(4)
  const [immersion, setImmersion] = useState(2)
  const [items, setItems] = useState([])
  const [errorText, setErrorText] = useState('')

  useEffect(()=> i18n.subscribe(setLang), [i18n])

  async function fetchRecs(e){
    e.preventDefault()
    setErrorText('')
    try {
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
      const data = await res.json().catch(()=> ({}))
      if (!res.ok) {
        setItems([])
        setErrorText(
          `API ${res.status}: ${data?.message || res.statusText} — ${JSON.stringify(data)}`
        )
        return
      }
      setItems(normalizeResponse(data))
    } catch (err) {
      console.error(err)
      setErrorText(String(err))
      setOuterError?.(err) // поднимем в ErrorBoundary
    }
  }

  function normalizeResponse(data){
    // 1) уже плоский формат
    if (Array.isArray(data?.items)) return data.items.map(it=>({
      season: Number(it.season),
      episode: Number(it.episode),
      title: it.title || '',
      reason: it.reason || 'selected'
    }))
    // 2) формат { recommendations: { "1":[...], "2":[...] } }
    if (data && data.recommendations && typeof data.recommendations === 'object'){
      const flat = []
      for (const [seasonStr, eps] of Object.entries(data.recommendations)){
        const season = Number(seasonStr)
        ;(eps || []).forEach(ep=>{
          flat.push({
            season,
            episode: Number(ep.episode ?? ep.number ?? 0),
            title: ep.title || '',
            reason: ep.reason || 'selected'
          })
        })
      }
      // опционально красиво отсортируем
      flat.sort((a,b)=> a.season - b.season || a.episode - b.episode)
      return flat
    }
    return []
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

      {errorText && (
        <pre style={{marginTop:16, padding:12, background:'#fff3cd', border:'1px solid #ffeeba', borderRadius:8, whiteSpace:'pre-wrap'}}>
          {errorText}
        </pre>
      )}

      <ul style={{marginTop:24}}>
        {items.map((it, idx)=>(
          <li key={`${it.season}-${it.episode}-${idx}`}>
            <strong>{t('seasonEp', it.season, it.episode)}</strong>
            {it.title ? ` — ${it.title} — `: ' — '}<i>{it.reason}</i>
          </li>
        ))}
      </ul>
    </div>
  )
}