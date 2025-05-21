// src/Pages/OrderTracking.tsx
import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import './OrderTracking.css';
import { getPackageOperations } from '../services/api';
import { PackageOperation } from '../types/dto';

interface TrackingStep {
  id: string;
  label: string;
  date: string | null;
  completed: boolean;
  description: string;
}

interface PackageStatus {
  id: number;
  orderId: number;
  status: 'processing' | 'packed' | 'loaded' | 'delivered';
  trackingNumber?: string;
  operations: PackageOperation[];
}

const OrderTracking: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();
  const [packageStatus, setPackageStatus] = useState<PackageStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchPackageInfo = async () => {
      if (!orderId) return;
      try {
        setLoading(true);
        const operations = await getPackageOperations(Number(orderId));
        if (!operations || operations.length === 0) {
          setError('No tracking information found for this order.');
          setLoading(false);
          return;
        }
        let currentStatus: 'processing' | 'packed' | 'loaded' | 'delivered' = 'processing';
        const hasPacked = operations.some(op => op.operation === 'PACK' && op.status === 'COMPLETED');
        const hasLoaded = operations.some(op => op.operation === 'LOAD' && op.status === 'COMPLETED');
        const hasDelivered = operations.some(op => op.operation === 'DELIVERED' && op.status === 'COMPLETED');
        if (hasDelivered) {
          currentStatus = 'delivered';
        } else if (hasLoaded) {
          currentStatus = 'loaded';
        } else if (hasPacked) {
          currentStatus = 'packed';
        }
        const status: PackageStatus = {
          id: Number(orderId),
          orderId: Number(orderId),
          status: currentStatus,
          trackingNumber: `UPS${orderId}`,
          operations
        };
        setPackageStatus(status);
        setError('');
      } catch {
        setError('Failed to load tracking information. Please try again later.');
      } finally {
        setLoading(false);
      }
    };
    fetchPackageInfo();
  }, [orderId]);

  const getTrackingSteps = (): TrackingStep[] => {
    if (!packageStatus) return [];
    const statusOrder = ['processing', 'packed', 'loaded', 'delivered'];
    const currentStatusIndex = statusOrder.indexOf(packageStatus.status);
    const packingOperation = packageStatus.operations.find(op => op.operation === 'PACK' && op.status === 'COMPLETED');
    const loadingOperation = packageStatus.operations.find(op => op.operation === 'LOAD' && op.status === 'COMPLETED');
    const deliveredOperation = packageStatus.operations.find(op => op.operation === 'DELIVERED' && op.status === 'COMPLETED');
    const steps: TrackingStep[] = [
      {
        id: 'processing',
        label: 'Processing',
        date: new Date().toISOString(),
        completed: currentStatusIndex >= 0,
        description: 'Order has been received and is being processed'
      },
      {
        id: 'packed',
        label: 'Packed',
        date: packingOperation ? new Date().toISOString() : null,
        completed: currentStatusIndex >= 1,
        description: 'Items have been packaged and are ready for pickup'
      },
      {
        id: 'loaded',
        label: 'Loaded',
        date: loadingOperation ? new Date().toISOString() : null,
        completed: currentStatusIndex >= 2,
        description: 'Package has been loaded onto delivery vehicle'
      },
      {
        id: 'delivered',
        label: 'Delivered',
        date: deliveredOperation ? new Date().toISOString() : null,
        completed: currentStatusIndex >= 3,
        description: 'Package has been delivered to the destination'
      }
    ];
    return steps;
  };

  if (loading) {
    return (
      <div className="order-tracking loading">
        <p>Loading tracking information...</p>
      </div>
    );
  }

  if (error || !packageStatus) {
    return (
      <div className="order-tracking error">
        <h1>Tracking Information Not Available</h1>
        <p>{error || 'Unable to find tracking information for this order.'}</p>
        <Link to="/" className="home-link">Return to Home</Link>
      </div>
    );
  }

  const trackingSteps = getTrackingSteps();

  return (
    <div className="order-tracking">
      <h1>Track Your Order</h1>
      <div className="tracking-info">
        <div className="info-box">
          <div className="info-row">
            <span className="info-label">Order Number:</span>
            <span className="info-value">#{packageStatus.orderId}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Tracking Number:</span>
            <span className="info-value">{packageStatus.trackingNumber}</span>
          </div>
          <div className="info-row">
            <span className="info-label">Status:</span>
            <span className="info-value status">{packageStatus.status}</span>
          </div>
        </div>
      </div>
      <div className="tracking-progress">
        <div className="progress-bar">
          <div
            className="progress-fill"
            style={{
              width: `${(trackingSteps.filter(step => step.completed).length - 1) / (trackingSteps.length - 1) * 100}%`
            }}
          ></div>
          {trackingSteps.map((step, index) => (
            <div
              key={step.id}
              className={`progress-step ${step.completed ? 'completed' : ''}`}
              style={{ left: `${index / (trackingSteps.length - 1) * 100}%` }}
            >
              <div className="step-dot"></div>
              <div className="step-label">{step.label}</div>
              {step.date && (
                <div className="step-date">
                  {new Date(step.date).toLocaleDateString()}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      <div className="tracking-history">
        <h2>Operation History</h2>
        <div className="history-timeline">
          {packageStatus.operations.map(operation => (
            <div key={operation.id} className="history-item">
              <div className="history-dot"></div>
              <div className="history-time">
                {new Date().toLocaleString()}
              </div>
              <div className="history-details">
                <h3>{operation.operation}</h3>
                <p>Status: {operation.status}</p>
                {operation.truckId && <p>Truck ID: {operation.truckId}</p>}
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="tracking-actions">
        <Link to="/" className="action-button">Continue Shopping</Link>
      </div>
    </div>
  );
};

export default OrderTracking;
