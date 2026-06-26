import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import ConsolidadoAportes from '../components/ConsolidadoAportes'

describe('ConsolidadoAportes component', () => {
  const mockConsultarConsolidado = vi.fn()

  beforeEach(() => {
    vi.mock('../api/aportesApi', () => ({
      consultarConsolidado: mockConsultarConsolidado,
    }))
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.unmock('../api/aportesApi')
  })

  it('should render the search form', () => {
    render(<ConsolidadoAportes />)
    expect(screen.getByPlaceholderText('AF-001')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('2025-01')).toBeInTheDocument()
    expect(screen.getByText('Consultar')).toBeInTheDocument()
  })

  it('should show table with results when data is available', async () => {
    const mockResponse = {
      afiliadoId: 'AF-001',
      periodoDesde: '2026-01',
      periodoHasta: '2026-06',
      totalAportado: 450000,
      detalle: [
        {
          id: '1',
          afiliadoId: 'AF-001',
          monto: 150000,
          fecha: '2026-01-15',
          canal: 'WEB',
          marcadaRevision: false,
        },
        {
          id: '2',
          afiliadoId: 'AF-001',
          monto: 300000,
          fecha: '2026-03-20',
          canal: 'APP_MOVIL',
          marcadaRevision: true,
        },
      ],
    }

    mockConsultarConsolidado.mockResolvedValueOnce(mockResponse)

    render(<ConsolidadoAportes />)

    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-001' } })
    fireEvent.change(screen.getByPlaceholderText('2025-01'), { target: { value: '2026-01' } })
    fireEvent.change(screen.getByPlaceholderText('2025-06'), { target: { value: '2026-06' } })

    fireEvent.click(screen.getByText('Consultar'))

    await waitFor(() => {
      expect(screen.getByText('AF-001')).toBeInTheDocument()
      expect(screen.getByText('$450,000')).toBeInTheDocument()
      expect(screen.getByText('2026-01-15')).toBeInTheDocument()
    })
  })

  it('should show "No se encontraron aportes" when there is no data', async () => {
    const mockResponse = {
      afiliadoId: 'AF-999',
      periodoDesde: '2026-01',
      periodoHasta: '2026-06',
      totalAportado: 0,
      detalle: [],
    }

    mockConsultarConsolidado.mockResolvedValueOnce(mockResponse)

    render(<ConsolidadoAportes />)

    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-999' } })
    fireEvent.change(screen.getByPlaceholderText('2025-01'), { target: { value: '2026-01' } })
    fireEvent.change(screen.getByPlaceholderText('2025-06'), { target: { value: '2026-06' } })

    fireEvent.click(screen.getByText('Consultar'))

    await waitFor(() => {
      expect(screen.getByText('No se encontraron aportes en el periodo indicado.')).toBeInTheDocument()
    })
  })
})