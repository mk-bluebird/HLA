import { useState } from "react";
import AccessSection from "./components/AccessSection";
import CodeSection from "./components/CodeSection";
import DeploySection from "./components/DeploySection";
import ErrorModel from "./components/ErrorModel";
import Footer from "./components/Footer";
import Masthead from "./components/Masthead";
import Pipeline from "./components/Pipeline";
import PrivacyTicker from "./components/PrivacyTicker";
import Simulator from "./components/Simulator";
import TopBar from "./components/TopBar";

export default function App() {
  const [live, setLive] = useState<{ error: number; inView: number } | null>(null);

  return (
    <div className="min-h-screen">
      <div className="noise-layer" aria-hidden />
      <TopBar />
      <main>
        <div className="bg-blueprint">
          <Masthead />
          <Simulator onTelemetry={(error, inView) => setLive({ error, inView })} />
        </div>
        <PrivacyTicker />
        <Pipeline />
        <ErrorModel live={live} />
        <AccessSection />
        <CodeSection />
        <DeploySection />
      </main>
      <Footer />
    </div>
  );
}
