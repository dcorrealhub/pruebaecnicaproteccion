import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import RegistrarAporte from './RegistrarAporte'
import { registrarAporte } from '../api/aportesApi'

vi.mock('../api/aportesApi', () => ({
  registrarAporte: vi.fn(),
}))

function llenarFormularioValido(container) {
  fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-1' } })
  fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '1000' } })
  return container.querySelector('form')
}

describe('<RegistrarAporte />', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('muestra el estado del aporte registrado y NO expone el id de la BD', async () => {
    registrarAporte.mockResolvedValue({ id: 42, estado: 'APROBADO', marcadaRevision: false })
    const { container } = render(<RegistrarAporte />)

    fireEvent.submit(llenarFormularioValido(container))

    expect(await screen.findByText(/Aporte registrado/i)).toBeInTheDocument()
    expect(screen.getByText(/APROBADO/)).toBeInTheDocument()
    expect(screen.queryByText(/42/)).not.toBeInTheDocument()
    expect(screen.queryByText(/ID:/i)).not.toBeInTheDocument()
  })

  it('rechaza monto no positivo sin llamar a la API', async () => {
    const { container } = render(<RegistrarAporte />)
    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-1' } })
    fireEvent.change(screen.getByRole('spinbutton'), { target: { value: '0' } })

    fireEvent.submit(container.querySelector('form'))

    expect(await screen.findByText(/mayor a cero/i)).toBeInTheDocument()
    expect(registrarAporte).not.toHaveBeenCalled()
  })

  it('avisa cuando el aporte queda marcado para revisión', async () => {
    registrarAporte.mockResolvedValue({ id: 7, estado: 'PENDIENTE_REVISION', marcadaRevision: true })
    const { container } = render(<RegistrarAporte />)

    fireEvent.submit(llenarFormularioValido(container))

    expect(await screen.findByText(/marcado para revisión/i)).toBeInTheDocument()
  })

  it('reutiliza la idempotenciaKey tras un fallo y genera una nueva tras el éxito', async () => {
    let intento = 0
    registrarAporte.mockImplementation(async () => {
      intento++
      if (intento === 1) throw new Error('falla de red')
      return { id: 99, estado: 'APROBADO', marcadaRevision: false }
    })
    const { container } = render(<RegistrarAporte />)
    const form = llenarFormularioValido(container)

    // Envío 1: falla -> la clave se conserva
    fireEvent.submit(form)
    await waitFor(() => expect(registrarAporte).toHaveBeenCalledTimes(1))
    await screen.findByText(/falla de red/i)

    // Envío 2: reintento -> reutiliza la misma clave y tiene éxito
    fireEvent.submit(form)
    await waitFor(() => expect(registrarAporte).toHaveBeenCalledTimes(2))
    await screen.findByText(/Aporte registrado/i)

    // Envío 3: nueva operación -> clave distinta
    fireEvent.submit(form)
    await waitFor(() => expect(registrarAporte).toHaveBeenCalledTimes(3))

    const claves = registrarAporte.mock.calls.map(c => c[0].idempotenciaKey)
    expect(claves[0]).toBe(claves[1])           // reintento idempotente
    expect(claves[1]).not.toBe(claves[2])        // operación nueva
  })
})
