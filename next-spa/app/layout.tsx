import { AuthProvider } from '@/context/AuthContext';
import './globals.css';
import type { Metadata } from 'next';

export const metadata: Metadata = {
  title: 'OAuth2.1 PKCE Demo',
  description: 'Minimal Next.js SPA with OAuth2.1 PKCE authentication',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
