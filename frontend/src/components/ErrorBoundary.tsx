import { Component, type ReactNode } from 'react'

interface Props { children: ReactNode }
interface State { hasError: boolean }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error) {
    console.error('Unhandled render error:', error)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex h-screen items-center justify-center bg-zinc-100">
          <div className="text-center">
            <p className="text-sm font-semibold text-zinc-900">Something went wrong</p>
            <p className="mt-1 text-xs text-zinc-500">Reload the page to continue.</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 bg-emerald-500 px-3 py-1.5 text-xs font-medium text-white hover:bg-emerald-600 transition-colors"
            >
              Reload
            </button>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
