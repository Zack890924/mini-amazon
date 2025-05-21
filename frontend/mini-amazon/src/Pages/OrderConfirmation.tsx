// src/Pages/OrderConfirmation.tsx
import React from 'react';
import { useParams, Link } from 'react-router-dom';
import './OrderConfirmation.css';

const OrderConfirmation: React.FC = () => {
  const { orderId } = useParams<{ orderId: string }>();

  return (
    <div className="order-confirmation">
      <div className="confirmation-header">
        <h1>Order Placed Successfully!</h1>
        <p>Thank you for your order. We've received your order and will begin processing it soon.</p>
      </div>
      
      <div className="order-info">
        <div className="info-section">
          <h2>Order Information</h2>
          <p><strong>Order Number:</strong> #{orderId}</p>
          <p><strong>Order Date:</strong> {new Date().toLocaleDateString()}</p>
          <p><strong>Order Status:</strong> <span className="status">Processing</span></p>
        </div>
      </div>
      
      <div className="next-steps">
        <p>Your order has been placed and will be processed soon. You can track your order status using the order number.</p>
        
        <div className="action-buttons">
          <Link to={`/order-tracking/${orderId}`} className="track-order-btn">
            Track My Order
          </Link>
          <Link to="/" className="continue-shopping-btn">
            Continue Shopping
          </Link>
        </div>
      </div>
    </div>
  );
};

export default OrderConfirmation;