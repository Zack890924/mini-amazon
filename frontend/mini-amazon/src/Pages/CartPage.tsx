// src/Pages/CartPage.tsx
import React from 'react';
import { Link } from 'react-router-dom';
import './CartPage.css';
import { useProductContext } from '../Contexts/ProductContext';

const CartPage: React.FC = () => {
  const { cart, removeFromCart, updateQuantity, getCartCount, getCartTotal } = useProductContext();

  return (
    <div className="cart-page">
      <h1>Shopping Cart</h1>
      
      {cart.length === 0 ? (
        <div className="empty-cart">
          <p>Your cart is empty</p>
          <Link to="/phone" className="continue-shopping">Continue Shopping</Link>
        </div>
      ) : (
        <>
          <div className="cart-items">
            {cart.map(item => (
              <div key={item.id} className="cart-item">
                <img src={item.image} alt={item.name} className="cart-item-image" />
                
                <div className="cart-item-details">
                  <h3>{item.name}</h3>
                  <p className="price-tag">${item.price}</p>
                  <p className="in-stock">In Stock</p>
                  
                  <div className="cart-item-actions">
                    <div className="quantity-selector">
                      <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>-</button>
                      <span>{item.quantity}</span>
                      <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
                    </div>
                    <button className="remove-btn" onClick={() => removeFromCart(item.id)}>
                      Remove
                    </button>
                  </div>
                </div>
                
                <div className="cart-item-total">
                  <p>${(parseInt(item.price) * item.quantity).toLocaleString()}</p>
                </div>
              </div>
            ))}
          </div>
          
          <div className="cart-summary">
            <div className="summary-row">
              <p>Subtotal ({getCartCount()} items):</p>
              <p className="price-tag">${getCartTotal().toLocaleString()}</p>
            </div>
            <Link to="/checkout" className="checkout-btn">Proceed to Checkout</Link>
          </div>
        </>
      )}
    </div>
  );
};

export default CartPage;