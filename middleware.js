// Vercel Edge Middleware for Markdown Content Negotiation (acceptmarkdown.com)
// Inspects 'Accept' header and rewrites/routes to markdown companion files for AI agents.

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - api routes (/api/*)
     * - static assets (.js, .css, .png, .jpg, .svg, .ico, .apk)
     * - well-known endpoints (/.well-known/*)
     */
    '/((?!api|_next|_vercel|assets|.*\\.(?:js|css|png|jpg|jpeg|webp|svg|ico|apk|xml|json)).*)',
  ],
};

const MARKDOWN_ROUTE_MAP = {
  '/': '/index.md',
  '/index': '/index.md',
  '/index.html': '/index.md',
  '/about': '/about.md',
  '/about.html': '/about.md',
  '/contact': '/contact.md',
  '/contact.html': '/contact.md',
  '/docs': '/docs.md',
  '/docs.html': '/docs.md',
  '/developers': '/docs.md',
  '/privacy': '/privacy.md',
  '/privacy.html': '/privacy.md',
  '/terms': '/terms.md',
  '/terms.html': '/terms.md',
  '/404': '/404.md',
  '/404.html': '/404.md'
};

export default function middleware(request) {
  const acceptHeader = request.headers.get('accept') || '';
  const url = new URL(request.url);
  const pathname = url.pathname.replace(/\/$/, '') || '/';

  const isMarkdownRequested =
    acceptHeader.includes('text/markdown') ||
    acceptHeader.includes('text/x-markdown') ||
    url.searchParams.get('format') === 'markdown';

  if (isMarkdownRequested) {
    const mdTarget = MARKDOWN_ROUTE_MAP[pathname] || MARKDOWN_ROUTE_MAP[`${pathname}.html`];

    if (mdTarget) {
      url.pathname = mdTarget;
      return new Response(null, {
        headers: {
          'x-middleware-rewrite': url.toString(),
          'Vary': 'Accept, Accept-Encoding',
          'Content-Type': 'text/markdown; charset=utf-8'
        }
      });
    }
  }

  // Pass through for standard HTML requests with Vary header
  return new Response(null, {
    headers: {
      'x-middleware-next': '1',
      'Vary': 'Accept, Accept-Encoding'
    }
  });
}
