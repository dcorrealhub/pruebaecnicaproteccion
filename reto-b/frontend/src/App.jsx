import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs'
import { Separator } from '@/components/ui/separator'
import RegistrarAporte from './components/RegistrarAporte'
import ConsolidadoAportes from './components/ConsolidadoAportes'

export default function App() {
  return (
    <div className="min-h-screen bg-secondary">
      <header className="bg-primary text-primary-foreground shadow-md">
        <div className="mx-auto max-w-4xl px-6 py-4">
          <p className="text-xs font-medium uppercase tracking-widest text-primary-foreground/70">
            Protección S.A. — Fondos de Inversión Voluntaria
          </p>
          <h1 className="mt-0.5 text-xl font-bold">Aportes Voluntarios</h1>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-6 py-8">
        <Tabs defaultValue="registrar">
          <TabsList className="mb-6 w-full justify-start bg-background shadow-sm">
            <TabsTrigger value="registrar" className="flex-1 sm:flex-none">
              Registrar aporte
            </TabsTrigger>
            <Separator orientation="vertical" className="mx-1 h-5" />
            <TabsTrigger value="consolidado" className="flex-1 sm:flex-none">
              Consolidado
            </TabsTrigger>
          </TabsList>

          <TabsContent value="registrar">
            <RegistrarAporte />
          </TabsContent>

          <TabsContent value="consolidado">
            <ConsolidadoAportes />
          </TabsContent>
        </Tabs>
      </main>
    </div>
  )
}
