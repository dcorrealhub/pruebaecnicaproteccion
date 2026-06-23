import { useState } from 'react';
import { consultarConsolidado, registrarAporte } from './api';

function periodoActual() {
  const now = new Date();
  const mes = String(now.getMonth() + 1).padStart(2, '0');
  return `${now.getFullYear()}-${mes}`;
}

export default function App() {
  const [registro, setRegistro] = useState({
    afiliadoId: '',
    monto: '',
    canal: 'WEB',
  });
  const [registroLoading, setRegistroLoading] = useState(false);
  const [registroError, setRegistroError] = useState('');
  const [registroOk, setRegistroOk] = useState('');

  const [consulta, setConsulta] = useState({
    afiliadoId: '',
    periodo: periodoActual(),
  });
  const [consultaLoading, setConsultaLoading] = useState(false);
  const [consultaError, setConsultaError] = useState('');
  const [consolidado, setConsolidado] = useState(null);

  async function handleRegistrar(e) {
    e.preventDefault();
    setRegistroLoading(true);
    setRegistroError('');
    setRegistroOk('');

    try {
      const idempotencyKey = crypto.randomUUID();
      const result = await registrarAporte(
        {
          afiliadoId: registro.afiliadoId.trim(),
          monto: Number(registro.monto),
          canal: registro.canal,
        },
        idempotencyKey
      );
      setRegistroOk(`Aporte registrado: id ${result.id}, estado ${result.estado}`);
      setRegistro((prev) => ({ ...prev, monto: '' }));
    } catch (err) {
      setRegistroError(err.message);
    } finally {
      setRegistroLoading(false);
    }
  }

  async function handleConsultar(e) {
    e.preventDefault();
    setConsultaLoading(true);
    setConsultaError('');
    setConsolidado(null);

    try {
      const data = await consultarConsolidado(
        consulta.afiliadoId.trim(),
        consulta.periodo.trim()
      );
      setConsolidado(data);
    } catch (err) {
      setConsultaError(err.message);
    } finally {
      setConsultaLoading(false);
    }
  }

  return (
    <main>
      <h1>Aportes voluntarios</h1>

      <section>
        <h2>Registrar aporte</h2>
        <form onSubmit={handleRegistrar}>
          <label>
            Afiliado ID
            <input
              value={registro.afiliadoId}
              onChange={(e) =>
                setRegistro({ ...registro, afiliadoId: e.target.value })
              }
              required
            />
          </label>
          <label>
            Monto
            <input
              type="number"
              min="0.01"
              step="0.01"
              value={registro.monto}
              onChange={(e) => setRegistro({ ...registro, monto: e.target.value })}
              required
            />
          </label>
          <label>
            Canal
            <select
              value={registro.canal}
              onChange={(e) => setRegistro({ ...registro, canal: e.target.value })}
            >
              <option value="WEB">WEB</option>
              <option value="MOVIL">MOVIL</option>
              <option value="OFICINA">OFICINA</option>
              <option value="CALL_CENTER">CALL_CENTER</option>
              <option value="ALIADO">ALIADO</option>
            </select>
          </label>
          <button type="submit" disabled={registroLoading}>
            {registroLoading ? 'Registrando...' : 'Registrar'}
          </button>
        </form>
        {registroError && <p className="error">{registroError}</p>}
        {registroOk && <p className="success">{registroOk}</p>}
      </section>

      <section>
        <h2>Consultar consolidado</h2>
        <form onSubmit={handleConsultar}>
          <label>
            Afiliado ID
            <input
              value={consulta.afiliadoId}
              onChange={(e) =>
                setConsulta({ ...consulta, afiliadoId: e.target.value })
              }
              required
            />
          </label>
          <label>
            Periodo (YYYY-MM)
            <input
              pattern="\d{4}-\d{2}"
              placeholder="2026-06"
              value={consulta.periodo}
              onChange={(e) =>
                setConsulta({ ...consulta, periodo: e.target.value })
              }
              required
            />
          </label>
          <button type="submit" disabled={consultaLoading}>
            {consultaLoading ? 'Consultando...' : 'Consultar'}
          </button>
        </form>
        {consultaError && <p className="error">{consultaError}</p>}

        {consolidado && (
          <div>
            <p className="resumen">
              Total: <strong>{consolidado.resumen.totalAportado}</strong> —{' '}
              {consolidado.resumen.cantidadAportes} aporte(s) en{' '}
              {consolidado.periodo.desde}
            </p>
            {consolidado.aportes.length === 0 ? (
              <p>Sin aportes en el periodo.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Fecha</th>
                    <th>Monto</th>
                    <th>Canal</th>
                    <th>Periodo</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {consolidado.aportes.map((a) => (
                    <tr key={a.id}>
                      <td>{a.id}</td>
                      <td>{a.fecha}</td>
                      <td>{a.monto}</td>
                      <td>{a.canal}</td>
                      <td>{a.periodo}</td>
                      <td>{a.estado}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </section>
    </main>
  );
}
