import { useRef, useState } from 'react'
import { AlertCircle, CheckCircle2, TriangleAlert } from 'lucide-react'
import { registrarAporte } from '../api/aportesApi'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

const HOY = new Date().toISOString().split('T')[0]

const TITULOS_ERROR = {
  TOPE_MENSUAL_EXCEDIDO: 'Tope mensual alcanzado',
  MONTO_INVALIDO: 'Monto no válido',
  CONFLICTO_CONCURRENCIA: 'Operación simultánea detectada',
  VALIDACION_FALLIDA: 'Datos incorrectos',
}

function tituloError(codigo) {
  return TITULOS_ERROR[codigo] ?? 'Error al registrar'
}

export default function RegistrarAporte() {
  const [form, setForm] = useState({ afiliadoId: '', monto: '', fecha: HOY, canal: 'APP_MOVIL' })
  const [errores, setErrores] = useState({})
  const [resultado, setResultado] = useState(null)
  const [errorServidor, setErrorServidor] = useState(null)
  const [cargando, setCargando] = useState(false)
  const idempotenciaKey = useRef(crypto.randomUUID())

  function campo(nombre) {
    return {
      value: form[nombre],
      onChange: e => {
        setForm(f => ({ ...f, [nombre]: e.target.value }))
        setErrores(prev => ({ ...prev, [nombre]: null }))
      },
    }
  }

  function validar() {
    const errs = {}
    if (!form.afiliadoId.trim()) errs.afiliadoId = 'El ID de afiliado es obligatorio'
    const monto = parseFloat(form.monto)
    if (!form.monto || isNaN(monto) || monto <= 0) errs.monto = 'Ingresá un monto mayor a cero'
    if (!form.fecha) errs.fecha = 'La fecha es obligatoria'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setErrorServidor(null)
    setResultado(null)

    const errs = validar()
    if (Object.keys(errs).length > 0) { setErrores(errs); return }

    setCargando(true)
    try {
      const data = await registrarAporte(
        { ...form, monto: parseFloat(form.monto) },
        idempotenciaKey.current
      )
      setResultado(data)
      idempotenciaKey.current = crypto.randomUUID()
    } catch (err) {
      setErrorServidor({ titulo: tituloError(err.codigo), mensaje: err.message, codigo: err.codigo })
    } finally {
      setCargando(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Registrar aporte</CardTitle>
        <CardDescription>
          Ingresá los datos del aporte al fondo de inversión voluntaria. Usá IDs sintéticos (ej. AF-001).
        </CardDescription>
      </CardHeader>

      <CardContent>
        <form onSubmit={handleSubmit} noValidate className="space-y-6">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="afiliadoId">ID Afiliado (sintético)</Label>
              <Input
                id="afiliadoId"
                placeholder="AF-001"
                data-testid="input-afiliado"
                className={errores.afiliadoId ? 'border-destructive focus-visible:ring-destructive' : ''}
                {...campo('afiliadoId')}
              />
              {errores.afiliadoId && (
                <p className="text-xs text-destructive">{errores.afiliadoId}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="monto">Monto (COP)</Label>
              <Input
                id="monto"
                type="number"
                min="0.01"
                step="0.01"
                placeholder="100000"
                data-testid="input-monto"
                className={errores.monto ? 'border-destructive focus-visible:ring-destructive' : ''}
                {...campo('monto')}
              />
              {errores.monto && (
                <p className="text-xs text-destructive">{errores.monto}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="fecha">Fecha del aporte</Label>
              <Input
                id="fecha"
                type="date"
                max={HOY}
                data-testid="input-fecha"
                className={errores.fecha ? 'border-destructive focus-visible:ring-destructive' : ''}
                {...campo('fecha')}
              />
              {errores.fecha && (
                <p className="text-xs text-destructive">{errores.fecha}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="canal">Canal de origen</Label>
              <Select
                value={form.canal}
                onValueChange={val => setForm(f => ({ ...f, canal: val }))}
              >
                <SelectTrigger id="canal" data-testid="select-canal">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="APP_MOVIL">App móvil</SelectItem>
                  <SelectItem value="WEB">Web</SelectItem>
                  <SelectItem value="SUCURSAL">Sucursal</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <Button type="submit" disabled={cargando} data-testid="btn-registrar" className="w-full sm:w-auto">
            {cargando ? 'Registrando…' : 'Registrar aporte'}
          </Button>
        </form>

        {errorServidor && (
          <Alert variant="destructive" className="mt-6">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>{errorServidor.titulo}</AlertTitle>
            <AlertDescription>
              {errorServidor.mensaje}
              {errorServidor.codigo === 'CONFLICTO_CONCURRENCIA' && (
                <p className="mt-1 font-medium">Por favor intentá de nuevo.</p>
              )}
            </AlertDescription>
          </Alert>
        )}

        {resultado && (
          <Alert variant="success" className="mt-6">
            <CheckCircle2 className="h-4 w-4" />
            <AlertTitle>Aporte registrado correctamente</AlertTitle>
            <AlertDescription className="space-y-2">
              <p>
                Periodo: <strong>{resultado.periodo}</strong>
              </p>
              {resultado.marcadaRevision && (
                <div className="flex items-start gap-2 rounded-md border border-orange-200 bg-orange-50 p-3">
                  <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0 text-orange-600" />
                  <div>
                    <p className="font-medium text-orange-800">Requiere revisión por cumplimiento</p>
                    <p className="text-sm text-orange-700">
                      El monto superó el umbral configurado. El área de cumplimiento debe revisar este aporte.
                    </p>
                  </div>
                </div>
              )}
            </AlertDescription>
          </Alert>
        )}
      </CardContent>
    </Card>
  )
}
