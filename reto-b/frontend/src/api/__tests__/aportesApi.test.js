import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { registrarAporte } from '../aportesApi'

const API_URL = '/api/aportes'

describe('registrarAporte()', () => {
  const mockData = {
    afiliadoId: 'afi-001',
    monto: 150000,
    canal: 'WEB',
    idempotenciaKey: 'idem-abc-123',
  }

  const mockResponse = {
    id: 'apo-001',
    afiliadoId: 'afi-001',
    monto: 150000,
    canal: 'WEB',
    estado: 'EXITOSO',
  }

  beforeEach(() => {
    // Mock global fetch before each test
    global.fetch = vi.fn()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('should call fetch with POST to /api/aportes', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await registrarAporte(mockData)

    expect(fetch).toHaveBeenCalledWith(API_URL, expect.objectContaining({
      method: 'POST',
    }))
  })

  it('should send afiliadoId, monto, canal, idempotenciaKey in the request body', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await registrarAporte(mockData)

    expect(fetch).toHaveBeenCalledWith(
      API_URL,
      expect.objectContaining({
        body: JSON.stringify(mockData),
      })
    )
  })

  it('should set Content-Type: application/json header', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    await registrarAporte(mockData)

    expect(fetch).toHaveBeenCalledWith(
      API_URL,
      expect.objectContaining({
        headers: {
          'Content-Type': 'application/json',
        },
      })
    )
  })

  it('should return the parsed JSON when fetch succeeds', async () => {
    global.fetch.mockResolvedValue({
      ok: true,
      json: async () => mockResponse,
    })

    const result = await registrarAporte(mockData)

    expect(result).toEqual(mockResponse)
  })

  it('should throw when fetch returns a non-OK status', async () => {
    global.fetch.mockResolvedValue({
      ok: false,
      status: 400,
      statusText: 'Bad Request',
    })

    await expect(registrarAporte(mockData)).rejects.toThrow()
  })

  it('should throw when fetch throws a network error', async () => {
    global.fetch.mockRejectedValue(new Error('Network Error'))

    await expect(registrarAporte(mockData)).rejects.toThrow('Network Error')
  })
})
