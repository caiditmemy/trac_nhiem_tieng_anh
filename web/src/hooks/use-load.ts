import { useEffect, useState } from "react"

import { errorText } from "@/lib/api"

/** Loads data once; `reload()` tries again after an error. Pass a function defined outside the component. */
export function useLoad<T>(load: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let cancelled = false
    load()
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(errorText(err))
      })
    return () => {
      cancelled = true
    }
  }, [load, version])

  return { data, error, reload: () => setVersion((value) => value + 1) }
}
