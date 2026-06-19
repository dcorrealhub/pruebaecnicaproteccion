import { useState } from 'react'
import { AlertCircle, Search, TrendingUp, TriangleAlert, CheckCircle2 } from 'lucide-react'
import { consultarConsolidado } from '../api/aportesApi'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

const PERIODO_ACTUAL = new Date().toISOString().slice(0, 7)
const PERIODO_INICIO_ANO = `${new Date().getFullYear()}-01`

function formatCOP(valor) {
  return Number(valor).toLocaleString('es-CO', {
    style: 'currency',
    currency: 'COP',
    minimumFractionDigits: 0,
  })
}

function formatFecha(fecha) {
  return new Date(fecha + 'T00:00:00').toLocaleDateString('es-CO', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

export default function ConsolidadoAportes() {
  const [filtros, setFiltros] = useState({
    afiliadoId: '',
    periodoDesde: PERIODO_INICIO_ANO,
    periodoHasta: PERIODO_ACTUAL,
  })
  const [consolidado, setConsolidado] = useState(null)
  const [error, setError] = useState(null)
  const [errores, setErrores] = useState({})
  const [cargando, setCargando] = useState(false)

  function campo(nombre) {
    return {
      value: filtros[nombre],
      onChange: e => {
        setFiltros(f => ({ ...f, [nombre]: e.target.value }))
        setErrores(prev => ({ ...prev, [nombre]: null }))
      },
    }
  }

  function validar() {
    const errs = {}
    if (!filtros.afiliadoId.trim()) {
      errs.afiliadoId = 'El ID de afiliado es obligatorio'
    }
    if (!filtros.periodoDesde) {
      errs.periodoDesde = 'El periodo de inicio es obligatorio'
    }
    if (!filtros.periodoHasta) {
      errs.periodoHasta = 'El periodo de fin es obligatorio'
    }
    if (filtros.periodoDesde && filtros.periodoHasta && filtros.periodoDesde > filtros.periodoHasta) {
      errs.periodoDesde = 'El periodo de inicio no puede ser posterior al periodo de fin'
    }
    return errs
  }

  async function handleBuscar(e) {
    e.preventDefault()
    setError(null)
    setConsolidado(null)

    const errs = validar()
    if (Object.keys(errs).length > 0) { setErrores(errs); return }

    setCargando(true)
    try {
      const data = await consultarConsolidado(filtros)
      setConsolidado(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Consultar consolidado</CardTitle>
          <CardDescription>
            Filtrá los aportes por afiliado y rango de periodos para ver el resumen.
          </CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleBuscar} noValidate>
            <div className="flex flex-wrap items-end gap-4">
              <div className="min-w-[180px] flex-1 space-y-2">
                <Label htmlFor="c-afiliado">ID Afiliado</Label>
                <Input
                  id="c-afiliado"
                  placeholder="AF-001"
                  data-testid="input-c-afiliado"
                  className={errores.afiliadoId ? 'border-destructive focus-visible:ring-destructive' : ''}
                  {...campo('afiliadoId')}
                />
                {errores.afiliadoId && (
                  <p className="text-xs text-destructive">{errores.afiliadoId}</p>
                )}
              </div>

              <div className="min-w-[150px] flex-1 space-y-2">
                <Label htmlFor="c-desde">Periodo desde</Label>
                <Input
                  id="c-desde"
                  type="month"
                  data-testid="input-c-desde"
                  className={errores.periodoDesde ? 'border-destructive focus-visible:ring-destructive' : ''}
                  {...campo('periodoDesde')}
                />
                {errores.periodoDesde && (
                  <p className="text-xs text-destructive">{errores.periodoDesde}</p>
                )}
              </div>

              <div className="min-w-[150px] flex-1 space-y-2">
                <Label htmlFor="c-hasta">Periodo hasta</Label>
                <Input
                  id="c-hasta"
                  type="month"
                  data-testid="input-c-hasta"
                  className={errores.periodoHasta ? 'border-destructive focus-visible:ring-destructive' : ''}
                  {...campo('periodoHasta')}
                />
                {errores.periodoHasta && (
                  <p className="text-xs text-destructive">{errores.periodoHasta}</p>
                )}
              </div>

              <Button type="submit" disabled={cargando} data-testid="btn-consultar">
                <Search className="mr-2 h-4 w-4" />
                {cargando ? 'Consultando…' : 'Consultar'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Error al consultar</AlertTitle>
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {consolidado && (
        <Card>
          <CardContent className="p-0">
            <div className="flex items-center justify-between border-b bg-primary px-6 py-4 rounded-t-xl">
              <div className="flex items-center gap-3">
                <div className="rounded-full bg-primary-foreground/10 p-2">
                  <TrendingUp className="h-5 w-5 text-primary-foreground" />
                </div>
                <div>
                  <p className="text-xs text-primary-foreground/70">Total aportado en el periodo</p>
                  <p className="text-2xl font-bold text-primary-foreground">
                    {formatCOP(consolidado.totalAportado)}
                  </p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-xs text-primary-foreground/70">Aportes</p>
                <p className="text-xl font-semibold text-primary-foreground">
                  {consolidado.detalle?.length ?? 0}
                </p>
              </div>
            </div>

            {consolidado.detalle?.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Fecha</TableHead>
                    <TableHead>Monto</TableHead>
                    <TableHead>Canal</TableHead>
                    <TableHead>Estado</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {consolidado.detalle.map(a => (
                    <TableRow key={a.id}>
                      <TableCell className="text-muted-foreground">{formatFecha(a.fecha)}</TableCell>
                      <TableCell className="font-medium">{formatCOP(a.monto)}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{a.canal.replace('_', ' ')}</Badge>
                      </TableCell>
                      <TableCell>
                        {a.marcadaRevision ? (
                          <Badge variant="warning" className="gap-1">
                            <TriangleAlert className="h-3 w-3" />
                            Revisión
                          </Badge>
                        ) : (
                          <Badge variant="success" className="gap-1">
                            <CheckCircle2 className="h-3 w-3" />
                            Normal
                          </Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <div className="flex flex-col items-center justify-center py-16 text-center">
                <Search className="mb-3 h-10 w-10 text-muted-foreground/40" />
                <p className="text-sm text-muted-foreground">
                  No se encontraron aportes en el periodo indicado.
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
