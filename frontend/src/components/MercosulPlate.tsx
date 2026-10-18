export default function MercosulPlate({ plate }: { plate: string }) {
  const formattedPlate = plate ? plate.toUpperCase().slice(0, 7) : "-------";

  return (
    <div className="mercosul-plate">
      <div className="mercosul-plate-header">
        <div className="mercosul-plate-logo">
          <span className="mercosul-plate-dots">&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;</span>
          <span className="mercosul-plate-logo-text">Mercosul</span>
        </div>
        <div className="mercosul-plate-country">BRASIL</div>
        <div className="mercosul-plate-flag">
          <div className="mercosul-plate-flag-diamond" />
          <div className="mercosul-plate-flag-circle" />
        </div>
      </div>
      <div className="mercosul-plate-body">
        <div className="mercosul-plate-side">
          <div className="mercosul-plate-qr" />
          <div className="mercosul-plate-br">BR</div>
        </div>
        <div className="mercosul-plate-number">{formattedPlate}</div>
      </div>
    </div>
  );
}
