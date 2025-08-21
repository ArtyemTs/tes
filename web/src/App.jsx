import React, { useState } from 'react'
const API_BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080"
export default function App() {
  const [showId, setShowId] = useState('got')
  const [targetSeason, setTargetSeason] = useState(4)
  const [immersion, setImmersion] = useState(2)
  const [items, setItems] = useState([])
  async function fetchRecs(e){
    e.preventDefault()
    const res = await fetch(`${API_BASE}/recommendations`, {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({ showId, targetSeason: Number(targetSeason), immersion: Number(immersion), locale:'en' })
    })
    const data = await res.json()
    setItems(data.items || [])
  }
  return (
    <div style={{maxWidth: 720, margin: '40px auto', fontFamily:'Inter, system-ui, sans-serif'}}>
      <h1>Through Every Season (MVP)</h1>
      <form onSubmit={fetchRecs} style={{display:'grid', gap:12}}>
        <label>Show ID <input value={showId} onChange={e=>setShowId(e.target.value)} /></label>
        <label>Target Season <input type="number" min="1" value={targetSeason} onChange={e=>setTargetSeason(e.target.value)} /></label>
        <label>Immersion (1..5) <input type="number" min="1" max="5" value={immersion} onChange={e=>setImmersion(e.target.value)} /></label>
        <button type="submit">Get recommendations</button>
      </form>
      <ul style={{marginTop:24}}>
        {items.map((it, idx)=>(
          <li key={idx}><strong>S{it.season}E{it.episode}</strong> {it.title ? `— ${it.title} — `: '— '}<i>{it.reason}</i></li>
        ))}
      </ul>
    </div>
  )
}
