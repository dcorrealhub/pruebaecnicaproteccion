/**
 * Muestra el error tal como lo devuelve el backend: mensaje + errores por campo (si los hay).
 */
export default function MensajeError({ error }) {
  if (!error) return null

  return (
    <div role="alert" className="alert alert--error">
      <p>
        <strong>Error{error.status ? ` ${error.status}` : ''}:</strong> {error.message}
      </p>
      {error.campos && (
        <ul>
          {Object.entries(error.campos).map(([campo, msg]) => (
            <li key={campo}><strong>{campo}:</strong> {msg}</li>
          ))}
        </ul>
      )}
    </div>
  )
}
