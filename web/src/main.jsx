import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'

function Root() {
  return (
    <App />
  )
}

// Ловим ошибки на уровне окна — чтобы не было «тихого» белого экрана
window.addEventListener('error', (e)=>{
  console.error('window error:', e.error || e.message || e)
})
window.addEventListener('unhandledrejection', (e)=>{
  console.error('unhandledrejection:', e.reason)
})

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
)