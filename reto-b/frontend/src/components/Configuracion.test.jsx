import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import Configuracion from './Configuracion'
import { obtenerParametros, actualizarParametros } from '../api/aportesApi'

vi.mock('../api/aportesApi', () => ({
  obtenerParametros: vi.fn(),
  actualizarParametros: vi.fn(),
}))

describe('<Configuracion />', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    obtenerParametros.mockResolvedValue({ topeMensual: 10000000, umbralRevision: 5000000 })
  })

  it('carga los parámetros actuales al montar', async () => {
    render(<Configuracion />)

    await waitFor(() => {
      const [tope, umbral] = screen.getAllByRole('spinbutton')
      expect(tope.value).toBe('10000000')
      expect(umbral.value).toBe('5000000')
    })
  })

  it('guarda los nuevos parámetros con los valores numéricos', async () => {
    actualizarParametros.mockResolvedValue({ topeMensual: 20000000, umbralRevision: 8000000 })
    const { container } = render(<Configuracion />)
    await waitFor(() => expect(screen.getAllByRole('spinbutton')).toHaveLength(2))

    const [tope, umbral] = screen.getAllByRole('spinbutton')
    fireEvent.change(tope, { target: { value: '20000000' } })
    fireEvent.change(umbral, { target: { value: '8000000' } })
    fireEvent.submit(container.querySelector('form'))

    await waitFor(() => expect(actualizarParametros).toHaveBeenCalledWith({
      topeMensual: 20000000,
      umbralRevision: 8000000,
    }))
    expect(await screen.findByText(/actualizados correctamente/i)).toBeInTheDocument()
  })

  it('valida en cliente que el umbral no supere el tope', async () => {
    const { container } = render(<Configuracion />)
    await waitFor(() => expect(screen.getAllByRole('spinbutton')).toHaveLength(2))

    const [tope, umbral] = screen.getAllByRole('spinbutton')
    fireEvent.change(tope, { target: { value: '1000' } })
    fireEvent.change(umbral, { target: { value: '2000' } })
    fireEvent.submit(container.querySelector('form'))

    expect(await screen.findByText(/no puede ser mayor que el tope/i)).toBeInTheDocument()
    expect(actualizarParametros).not.toHaveBeenCalled()
  })
})
