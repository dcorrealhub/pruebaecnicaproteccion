export default function Modal({ tipo, titulo, children, onClose }) {
  return (
    <div style={s.overlay} onClick={onClose}>
      <div style={s.modal} onClick={e => e.stopPropagation()}>
        <div style={{ ...s.header, ...s.headerTipo[tipo] }}>
          <span style={s.icon}>{iconos[tipo]}</span>
          <h3 style={s.titulo}>{titulo}</h3>
          <button onClick={onClose} style={s.closeBtn}>✕</button>
        </div>
        <div style={s.body}>{children}</div>
        <div style={s.footer}>
          <button onClick={onClose} style={{ ...s.btn, ...s.btnTipo[tipo] }}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  )
}

const iconos = {
  success: '✓',
  error:   '✕',
  warning: '⚠',
  info:    'ℹ',
}

const s = {
  overlay: {
    position: 'fixed', inset: 0,
    background: 'rgba(0,0,0,0.45)',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    zIndex: 1000,
  },
  modal: {
    background: '#fff', borderRadius: 12,
    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
    width: '100%', maxWidth: 460,
    overflow: 'hidden',
  },
  header: {
    display: 'flex', alignItems: 'center', gap: 12,
    padding: '16px 20px',
  },
  headerTipo: {
    success: { background: '#F0FDF4', borderBottom: '1px solid #BBF7D0' },
    error:   { background: '#FEF2F2', borderBottom: '1px solid #FECACA' },
    warning: { background: '#FFFBEB', borderBottom: '1px solid #FDE68A' },
    info:    { background: '#EFF6FF', borderBottom: '1px solid #BFDBFE' },
  },
  icon: { fontSize: 20, fontWeight: 700 },
  titulo: { flex: 1, margin: 0, fontSize: 16, fontWeight: 700, color: '#111827' },
  closeBtn: {
    background: 'none', border: 'none',
    fontSize: 16, cursor: 'pointer',
    color: '#6B7280', padding: 4,
  },
  body: { padding: '20px 24px', fontSize: 14, color: '#374151', lineHeight: 1.6 },
  footer: {
    padding: '12px 24px 20px',
    display: 'flex', justifyContent: 'flex-end',
  },
  btn: {
    padding: '9px 24px', border: 'none',
    borderRadius: 6, fontSize: 14,
    fontWeight: 600, cursor: 'pointer',
  },
  btnTipo: {
    success: { background: '#6CB531', color: '#fff' },
    error:   { background: '#E65014', color: '#fff' },
    warning: { background: '#FFB71B', color: '#111' },
    info:    { background: '#00A0DF', color: '#fff' },
  },
}