import { labels } from '@/lib/presentation/labels';

export default function Footer() {
  return (
    <footer className="app-footer">
      <div className="footer-content">
        <span className="footer-developer">
          {labels.footer.developedBy}{' '}
          <a
            href={labels.footer.portfolioLink}
            target="_blank"
            rel="noopener noreferrer"
            className="footer-link"
          >
            {labels.footer.developerName}
          </a>
        </span>
        <span className="footer-separator">·</span>
        <span className="footer-copyright">{labels.footer.copyright}</span>
        <span className="footer-separator">·</span>
        <span className="footer-version">{labels.footer.version}</span>
      </div>
    </footer>
  );
}
