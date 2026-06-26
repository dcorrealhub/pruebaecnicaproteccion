import '@testing-library/jest-dom'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Limpia el DOM renderizado entre tests para evitar fugas de estado.
afterEach(() => {
  cleanup()
})
