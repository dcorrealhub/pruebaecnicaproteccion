export async function login(username: string, password: string): Promise<string> {
  const response = await fetch('/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })

  if (!response.ok) {
    throw new Error('Credenciales inválidas')
  }

  const data = await response.json() as { token: string }
  return data.token
}
