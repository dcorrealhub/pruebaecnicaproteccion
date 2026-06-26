import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import RegistrarAporte from '../components/RegistrarAporte'

describe('RegistrarAporte component', () => {
  const mockRegistarAporte = vi.fn()

  beforeEach(() => {
    vi.mock('../api/aportesApi', () => ({
      registrarAporte: mockRegistarAporte,
    }))
  })

  afterEach(() => {
    vi.clearAllMocks()
    vi.unmock('../api/aportesApi')
  })

  it('should render the form', () => {
    render(<RegistrarAporte />)
    expect(screen.getByPlaceholderText('AF-001')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Monto (COP)')).toBeInTheDocument()
    expect(screen.getByText('Registrar')).toBeInTheDocument()
  })

  it('should validate monto > 0 before sending', () => {
    render(<RegistrarAporte />)

    fireEvent.change(screen.getByPlaceholderText('Monto (COP)'), { target: { value: '0' } })
    fireEvent.click(screen.getByText('Registrar'))

    expect(mockRegistarAporte).not.toHaveBeenCalled()
  })

  it('should show success message when aporte is registered', async () => {
    const mockResponse = {
      id: '1',
      afiliadoId: 'AF-001',
      monto: 100000,
      canal: 'APP_MOVIL',
      marcadaRevision: false,
    }

    mockRegistarAporte.mockResolvedValueOnce(mockResponse)

    render(<RegistrarAporte />)

    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-001' } })
    fireEvent.change(screen.getByPlaceholderText('Monto (COP)'), { target: { value: '100000' } })
    fireEvent.click(screen.getByText('Registrar'))

    await waitFor(() => {
      expect(screen.getByText('Aporte registrado. ID: 1')).toBeInTheDocument()
    })
  })

  it('should show alert if the aporte queda marked for review', async () => {
    const mockResponse = {
      id: '2',
      afiliadoId: 'AF-002',
      monto: 6000000,
      canal: 'APP_MOVIL',
      marcadaRevision: true,
    }

    mockRegistarAporte.mockResolvedValueOnce(mockResponse)

    render(<RegistrarAporte />)

    fireEvent.change(screen.getByPlaceholderText('AF-001'), { target: { value: 'AF-002' } })
    fireEvent.change(screen.getByPlaceholderText('Monto (COP)'), { target: { value: '6000000' } })
    fireEvent.click(screen.getByText('Registrar'))

    await waitFor(() => {
      expect(screen.getByText('Este aporte quedó marcado para revisión.')).toBeInTheDocument()
    })
  })
})