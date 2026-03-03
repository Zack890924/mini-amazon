// src/Pages/CheckoutPage.tsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './CheckOutPage.css';
import { useProductContext } from '../Contexts/ProductContext';
import { createOrder } from '../services/api';

interface DeliveryCoordinates{
  x: string;
  y: string;
}

const CheckoutPage: React.FC = () => {
  const navigate = useNavigate();
  const { cart, clearCart, getCartTotal } = useProductContext();
  
  const [coordinates, setCoordinates] = useState<DeliveryCoordinates>({
    x: '',
    y: ''
  });
  
  const [upsAccount, setUpsAccount] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setCoordinates({
      ...coordinates,
      [name]: value
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    
    if(isNaN(Number(coordinates.x)) || isNaN(Number(coordinates.y))){
      setError('Please enter valid numeric coordinates');
      return;
    }
    
    if(cart.length === 0){
      setError('Your cart is empty');
      return;
    }
    
    setIsLoading(true);
    setError('');
    
    try {
   
      const result = await createOrder(cart, coordinates, upsAccount || undefined);
      
     
      clearCart();
      
     
      navigate(`/order-confirmation/${result.orderId}`);
    } catch (error){
      console.error('Error creating order:', error);
      setError('Failed to create order. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };


  if(cart.length === 0){
    return (
      <div className="checkout-page">
        <h1>Checkout</h1>
        <div className="empty-checkout">
          <p>Your cart is empty. Please add some items before checkout.</p>
          <button onClick={() => navigate('/phone')} className="continue-shopping-btn">
            Continue Shopping
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="checkout-page">
      <h1>Checkout</h1>
      
      {error && <div className="error-message">{error}</div>}
      
      <div className="checkout-container">
        <form className="checkout-form" onSubmit={handleSubmit}>
          <div className="form-section">
            <h2>Delivery Coordinates</h2>
            
            <div className="form-row">
              <div className="form-group">
                <label htmlFor="x">X Coordinate</label>
                <input
                  type="text"
                  id="x"
                  name="x"
                  value={coordinates.x}
                  onChange={handleInputChange}
                  placeholder="Enter X coordinate"
                  required
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="y">Y Coordinate</label>
                <input
                  type="text"
                  id="y"
                  name="y"
                  value={coordinates.y}
                  onChange={handleInputChange}
                  placeholder="Enter Y coordinate"
                  required
                />
              </div>
            </div>
          </div>
          
          <div className="form-section">
            <h2>UPS Account (Optional)</h2>
            <div className="form-group">
              <label htmlFor="upsAccount">UPS Account Number</label>
              <input
                type="text"
                id="upsAccount"
                value={upsAccount}
                onChange={(e) => setUpsAccount(e.target.value)}
                placeholder="Leave blank if you don't have a UPS account"
              />
            </div>
          </div>
          
          <button 
            type="submit" 
            className="place-order-btn"
            disabled={isLoading}
          >
            {isLoading ? 'Processing...' : 'Place Order'}
          </button>
        </form>
        
        <div className="order-summary">
          <h2>Order Summary</h2>
          
          <div className="summary-items">
            {cart.map(item => (
              <div key={item.id} className="summary-item">
                <img src={item.image} alt={item.name} />
                <div className="summary-item-details">
                  <h3>{item.name}</h3>
                  <p>Qty: {item.quantity}</p>
                  <p className="price-tag">${(parseInt(item.price) * item.quantity).toLocaleString()}</p>
                </div>
              </div>
            ))}
          </div>
          
          <div className="summary-total">
            <p>Total:</p>
            <p className="price-tag">${getCartTotal().toLocaleString()}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;