import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { Canvas, useFrame } from '@react-three/fiber';
import { OrbitControls, Float, Text3D, Center, useMatcapTexture } from '@react-three/drei';
import * as THREE from 'three';

// Animated 3D "404" Text
function AnimatedText({ position }) {
    const textRef = useRef();
    const [matcap] = useMatcapTexture('7B5254_E9DCC7_B19986_C8AC91', 256);

    useFrame((state) => {
        if (textRef.current) {
            textRef.current.rotation.y = Math.sin(state.clock.elapsedTime * 0.5) * 0.2;
            textRef.current.position.y = Math.sin(state.clock.elapsedTime * 0.8) * 0.3;
        }
    });

    return (
        <group position={position}>
            <Center>
                <Text3D
                    ref={textRef}
                    font="/fonts/helvetiker_bold.typeface.json"
                    size={2}
                    height={0.5}
                    curveSegments={12}
                    bevelEnabled
                    bevelThickness={0.1}
                    bevelSize={0.05}
                    bevelOffset={0}
                    bevelSegments={5}
                >
                    404
                    <meshBasicMaterial color="#00ff00" />
                </Text3D>
            </Center>
        </group>
    );
}

// Floating Geometric Shapes
function FloatingShape({ position, geometry, color }) {
    const meshRef = useRef();

    useFrame((state) => {
        if (meshRef.current) {
            meshRef.current.rotation.x += 0.01;
            meshRef.current.rotation.y += 0.01;
        }
    });

    return (
        <Float speed={2} rotationIntensity={1} floatIntensity={2}>
            <mesh ref={meshRef} position={position}>
                {geometry}
                <meshStandardMaterial color={color} wireframe />
            </mesh>
        </Float>
    );
}

// Background Particles
function Particles() {
    const particlesRef = useRef();
    const count = 100;

    const positions = new Float32Array(count * 3);
    for (let i = 0; i < count * 3; i++) {
        positions[i] = (Math.random() - 0.5) * 20;
    }

    useFrame((state) => {
        if (particlesRef.current) {
            particlesRef.current.rotation.y = state.clock.elapsedTime * 0.05;
        }
    });

    return (
        <points ref={particlesRef}>
            <bufferGeometry>
                <bufferAttribute
                    attach="attributes-position"
                    count={count}
                    array={positions}
                    itemSize={3}
                />
            </bufferGeometry>
            <pointsMaterial size={0.05} color="#10b981" transparent opacity={0.6} />
        </points>
    );
}

export default function NotFound() {
    const canvasRef = useRef();

    useEffect(() => {
        // GSAP animation for text elements
        const elements = document.querySelectorAll('.fade-in');
        elements.forEach((el, index) => {
            setTimeout(() => {
                el.style.opacity = '1';
                el.style.transform = 'translateY(0)';
            }, index * 200);
        });
    }, []);

    return (
        <div className="min-h-screen bg-gradient-to-br from-gray-900 via-black to-gray-900 flex items-center justify-center relative overflow-hidden">
            {/* Three.js Canvas Background */}
            <div className="absolute inset-0 z-0">
                <Canvas
                    ref={canvasRef}
                    camera={{ position: [0, 0, 8], fov: 75 }}
                    style={{ background: 'transparent' }}
                >
                    <ambientLight intensity={0.5} />
                    <pointLight position={[10, 10, 10]} intensity={1} />
                    <pointLight position={[-10, -10, -10]} intensity={0.5} color="#10b981" />



                    {/* Background Particles */}
                    <Particles />

                    {/* Controls */}
                    <OrbitControls
                        enableZoom={false}
                        enablePan={false}
                        autoRotate
                        autoRotateSpeed={0.5}
                    />
                </Canvas>
            </div>

            {/* Content Overlay */}
            <div className="relative z-10 text-center px-4 sm:px-6 max-w-2xl">
                {/* Glassmorphism Card */}
                <div className="bg-gradient-to-br from-white/5 to-white/2 backdrop-blur-xl border border-green-500/20 rounded-2xl sm:rounded-3xl p-6 sm:p-8 lg:p-12 shadow-2xl">
                    <h1
                        className="text-5xl sm:text-6xl lg:text-8xl font-bold bg-gradient-to-r from-green-400 via-emerald-500 to-green-600 bg-clip-text text-transparent mb-4 sm:mb-6 fade-in"
                        style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}
                    >
                        Oops!
                    </h1>

                    <h2
                        className="text-xl sm:text-2xl lg:text-3xl font-semibold text-gray-100 mb-3 sm:mb-4 fade-in"
                        style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}
                    >
                        Page Not Found
                    </h2>

                    <p
                        className="text-gray-400 text-lg mb-8 fade-in"
                        style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}
                    >
                        The page you're looking for seems to have wandered off into the digital void.
                        Don't worry, even the best coders get lost sometimes!
                    </p>

                    {/* Action Buttons */}
                    <div
                        className="flex flex-col sm:flex-row gap-4 justify-center fade-in"
                        style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}
                    >
                        <Link
                            to="/"
                            className="px-8 py-4 bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-semibold rounded-xl shadow-lg shadow-green-500/30 transition-all transform hover:scale-105"
                        >
                            Go Home
                        </Link>

                        <Link
                            to="/contests"
                            className="px-8 py-4 bg-white/10 hover:bg-white/20 border border-white/20 hover:border-green-500/50 text-gray-100 font-semibold rounded-xl transition-all transform hover:scale-105"
                        >
                            View Contests
                        </Link>
                    </div>

                    {/* Fun Error Code */}
                    <div className="mt-8 pt-8 border-t border-white/10 fade-in" style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}>
                        <p className="text-sm text-gray-500 font-mono">
                            Error Code: <span className="text-green-400">404_PAGE_NOT_FOUND</span>
                        </p>
                        <p className="text-xs text-gray-600 mt-2">
                            "In the world of coding, every error is just an opportunity to debug life."
                        </p>
                    </div>
                </div>

                {/* Floating Hints */}
                <div className="mt-6 sm:mt-8 flex flex-col sm:flex-row justify-center gap-2 sm:gap-4 text-xs sm:text-sm text-gray-500 fade-in" style={{ opacity: 0, transform: 'translateY(20px)', transition: 'all 0.8s ease-out' }}>
                    <span> Tip: Check the URL</span>
                    <span>•</span>
                    <span> Or use navigation</span>
                </div>
            </div>

            {/* Animated Background Gradient Orbs */}
            <div className="absolute top-20 left-20 w-72 h-72 bg-green-500/20 rounded-full blur-3xl animate-pulse"></div>
            <div className="absolute bottom-20 right-20 w-96 h-96 bg-emerald-500/20 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }}></div>
        </div>
    );
}
