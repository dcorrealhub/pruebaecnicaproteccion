import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { consultarConsolidado } from '../aportesApi'

const API_URL = '/api/aportes'

describe('consultarConsolidado()', () => {
  const mockData = {
    afiliadoId: 'afi-001',
    periodoDesde: '2026-01',
    periodoHasta: '2026-06',
  }

  const mockResponse = {
    afiliadoId: 'afi-001',
    periodoDesde: '2026-01',
    periodoHasta: '2026-06',
    totalAportado: 500000,
    detalle: [
      {
        id: 'apo-001',
        afiliadoId: 'afi-001',
        monto: 150000,
        fecha: '2026-01-15',
        canal: 'WEB',
        marcadaRevision: false,
      },
      {
        id: 'apo-002',
        afiliadoId: 'afi-001',
        monto: 350000,
        fecha: '2026-03-20',
        canal: 'APP_MOVIL',
        marcadaRevision: false,
      },
    ],
  }

  beforeEach(() => {
    global.fetch = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should call fetch with GET to /api/aportes/consolidado', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await consultarConsolidado(mockData)

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(API_URL + '/consolidado'),
      expect.objectContaining({
        method: 'GET',
      })
    )
  })

  it('should include afiliadoId, periodoDesde, periodoHasta in query params', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await consultarConsolidado(mockData)

    expect(fetch).toHaveBeenCalledWith(
      API_URL + '/consolidado?afiliadoId=' + encodeURIComponent(mockData.afiliadoId) +
        '&periodoDesde=' + encodeURIComponent(mockData.periodoDesde) +
        '&periodoHasta=' + encodeURIComponent(mockData.periodoHasta),
      expect.objectContaining({})
    )
  })

  it('should set no headers when not needed', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await consultarConsolidado(mockData)

    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining(API_URL + '/consolidado'),
      expect.not.objectContaining({ headers: expect.any(Object) })
    )
  })

  it('should return the parsed JSON when fetch succeeds', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    const result = await consultarConsolidado(mockData)

    expect(result).toEqual(mockResponse)
  })

  it('should throw when fetch returns a non-OK status', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
    })

    await expect(consultarConsolidado(mockData)).rejects.toThrow()
  })

  it('should throw when fetch throws a network error', async () => {
    global.fetch.mockRejectedValue(new Error('Network Error'))

    await expect(consultarConsolidado(mockData)).rejects.toThrow('Network Error')
  })
})