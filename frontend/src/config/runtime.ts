interface RuntimeConfig {
  API_GROUP: string
  [key: string]: string
}

export function getRuntimeConfig(): RuntimeConfig {
  if (typeof window !== 'undefined' && window.__runtime_config__) {
    return window.__runtime_config__ as RuntimeConfig
  }
  return { API_GROUP: '/api/v1' }
}

export function getServiceHost(serviceName: string): string {
  const config = getRuntimeConfig()
  return config[`${serviceName.toUpperCase()}_HOST`] ?? ''
}
