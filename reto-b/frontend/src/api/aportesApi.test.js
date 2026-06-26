import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import {
  registrarAporte,
  consultarConsolidado,
  resolverAporte,
  obtenerParametros,
  actualizarParametros,
} from './aportesApi'

function mockFetchOnce({ ok = true, status = 200, body = {} }) {
  return vi.fn().mockResolvedValue({
    ok,
    status,
    json: () => Promise.resolve(body),
  })
}

describe('aportesApi', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('registrarAporte hace POST con el cuerpo y devuelve el aporte', async () => {
    const fetchMock = mockFetchOnce({ status: 201, body: { id: 1, estado: 'APROBADO' } })
    vi.stubGlobal('fetch', fetchMock)

    const data = await registrarAporte({ afiliadoId: 'AF-1', monto: 1000, canal: 'WEB', idempotenciaKey: 'k1' })

    expect(data).toEqual({ id: 1, estado: 'APROBADO' })
    const [url, opts] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/aportes')
    expect(opts.method).toBe('POST')
    expect(JSON.parse(opts.body)).toMatchObject({ afiliadoId: 'AF-1', idempotenciaKey: 'k1' })
  })

  it('consultarConsolidado arma el query string con los filtros', async () => {
    const fetchMock = mockFetchOnce({ body: { totalAportado: 0 } })
    vi.stubGlobal('fetch', fetchMock)

    await consultarConsolidado({ afiliadoId: 'AF-9', periodoDesde: '2025-01', periodoHasta: '2025-06' })

    const url = fetchMock.mock.calls[0][0]
    expect(url).toContain('/api/aportes/consolidado?')
    expect(url).toContain('afiliadoId=AF-9')
    expect(url).toContain('periodoDesde=2025-01')
    expect(url).toContain('periodoHasta=2025-06')
  })

  it('resolverAporte llama al endpoint de la acción con POST', async () => {
    const fetchMock = mockFetchOnce({ body: { id: 5, estado: 'APROBADO' } })
    vi.stubGlobal('fetch', fetchMock)

    await resolverAporte(5, 'aprobar')

    const [url, opts] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/aportes/5/aprobar')
    expect(opts.method).toBe('POST')
  })

  it('propaga el mensaje del backend cuando la respuesta no es ok', async () => {
    const fetchMock = mockFetchOnce({
      ok: false,
      status: 422,
      body: { mensaje: 'El aporte supera el tope mensual del afiliado' },
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(registrarAporte({})).rejects.toThrow('El aporte supera el tope mensual del afiliado')
  })

  it('arma el detalle por campo cuando hay errores de validación', async () => {
    const fetchMock = mockFetchOnce({
      ok: false,
      status: 400,
      body: { mensaje: 'Datos inválidos', errores: { monto: 'El monto debe ser mayor a cero' } },
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(registrarAporte({})).rejects.toThrow(/monto: El monto debe ser mayor a cero/)
  })

  it('obtenerParametros y actualizarParametros usan el endpoint de configuración', async () => {
    const fetchMock = mockFetchOnce({ body: { topeMensual: 10000000, umbralRevision: 5000000 } })
    vi.stubGlobal('fetch', fetchMock)

    await obtenerParametros()
    expect(fetchMock.mock.calls[0][0]).toBe('/api/configuracion/parametros')

    await actualizarParametros({ topeMensual: 20000000, umbralRevision: 8000000 })
    const [url, opts] = fetchMock.mock.calls[1]
    expect(url).toBe('/api/configuracion/parametros')
    expect(opts.method).toBe('PUT')
    expect(JSON.parse(opts.body)).toEqual({ topeMensual: 20000000, umbralRevision: 8000000 })
  })
})
