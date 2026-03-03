// src/Pages/OrderHistoryPage.tsx
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import './OrderHistoryPage.css';

interface Order {
  orderId: number;
  orderDate: string;
  status: string;
  trackingNumber: string;
  destinationX: number;
  destinationY: number;
}

const OrderHistoryPage: React.FC = () => {
  const [upsAccount, setUpsAccount] = useState('');
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setUpsAccount(e.target.value);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if(!upsAccount.trim()){
      setError('Please enter a UPS account number');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
      const response = await fetch(`http://localhost:8080/api/orders/account/${upsAccount}`);
      
      if(!response.ok){
        throw new Error(`Error: ${response.status}`);
      }
      
      const data = await response.json();
      setOrders(data);
      setSearched(true);
    } catch (err){
      console.error('Error fetching orders:', err);
      setError('Failed to fetch orders. Please try again.');
      setOrders([]);
      setSearched(true);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="order-history-page">
      <h1>Find Your Orders</h1>
      
      <div className="search-section">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="upsAccount">UPS Account Number</label>
            <input
              type="text"
              id="upsAccount"
              value={upsAccount}
              onChange={handleInputChange}
              placeholder="Enter your UPS account number"
              required
            />
          </div>
          
          <button 
            type="submit" 
            className="search-btn"
            disabled={isLoading}
          >
            {isLoading ? 'Searching...' : 'Find Orders'}
          </button>
        </form>
      </div>
      
      {error && <div className="error-message">{error}</div>}
      
      {searched && (
        <div className="results-section">
          <h2>Orders for UPS Account: {upsAccount}</h2>
          
          {orders.length === 0 ? (
            <p className="no-orders">No orders found for this UPS account.</p>
          ) : (
            <div className="orders-list">
              <div className="order-header">
                <span>Order ID</span>
                <span>Date</span>
                <span>Status</span>
                <span>Tracking Number</span>
                <span>Actions</span>
              </div>
              
              {orders.map(order => (
                <div key={order.orderId} className="order-item">
                  <span>#{order.orderId}</span>
                  <span>{new Date(order.orderDate).toLocaleDateString()}</span>
                  <span className={`status ${order.status.toLowerCase()}`}>{order.status}</span>
                  <span>{order.trackingNumber}</span>
                  <span>
                    <Link to={`/order-tracking/${order.orderId}`} className="track-btn">
                      Track
                    </Link>
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default OrderHistoryPage;