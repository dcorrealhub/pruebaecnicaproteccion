import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import ConsolidadoAportes from './ConsolidadoAportes'
import { consultarConsolidado, resolverAporte } from '../api/aportesApi'

vi.mock('../api/aportesApi', () => ({
  consultarConsolidado: vi.fn(),
  resolverAporte: vi.fn(),
}))

const COP = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 2 })

describe('<ConsolidadoAportes />', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('precarga el rango de periodos (1 año hasta el mes actual)', () => {
    const { container } = render(<ConsolidadoAportes />)
    const meses = container.querySelectorAll('input[type="month"]')

    expect(meses).toHaveLength(2)
    const [desde, hasta] = meses
    expect(hasta.value).toMatch(/^\d{4}-\d{2}$/)
    expect(desde.value).toMatch(/^\d{4}-\d{2}$/)
    // 'desde' es anterior a 'hasta'
    expect(desde.value < hasta.value).toBe(true)
  })

  it('muestra el total comprometido (aprobado + en revisión)', async () => {
    consultarConsolidado.mockResolvedValue({
      totalAportado: 1000000,
      totalEnRevision: 500000,
      detalle: [],
    })
    const { container } = render(<ConsolidadoAportes />)
    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-1' } })

    fireEvent.submit(container.querySelector('form'))

    const comprometido = (await screen.findByText(/Total comprometido/i)).closest('p')
    expect(comprometido.textContent).toContain(COP.format(1500000))
  })

  it('permite aprobar un aporte pendiente desde la tabla', async () => {
    consultarConsolidado.mockResolvedValue({
      totalAportado: 0,
      totalEnRevision: 6000000,
      detalle: [
        { id: 11, fecha: '2026-06-10', monto: 6000000, canal: 'WEB', estado: 'PENDIENTE_REVISION' },
      ],
    })
    resolverAporte.mockResolvedValue({ id: 11, estado: 'APROBADO' })
    const { container } = render(<ConsolidadoAportes />)
    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-1' } })
    fireEvent.submit(container.querySelector('form'))

    const aprobar = await screen.findByRole('button', { name: /aprobar/i })
    fireEvent.click(aprobar)

    await waitFor(() => expect(resolverAporte).toHaveBeenCalledWith(11, 'aprobar'))
  })
})
