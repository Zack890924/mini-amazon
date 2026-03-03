import React from 'react';
import { Routes, Route } from 'react-router-dom';
import PhonePage from './Components/PhonePage/PhonePage';
import OrderDetailPage from './Components/OrderDetailPage/OrderDetailPage';
import MainPage from './HomePage/MainPage';
import Navbar from './Components/Navbar';
import CartPage from './Pages/CartPage';
// import { CartProvider } from './Contexts/CartContext';
import { ProductProvider } from './Contexts/ProductContext';
import CheckoutPage from './Pages/CheckOutPage';
import OrderConfirmation from './Pages/OrderConfirmation';
import OrderTracking from './Pages/OrderTracking';

import OrderHistoryPage from './Pages/OrderHistoryPage';
function App(){
  return (
    <ProductProvider>
      <Navbar />
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/phone" element={<PhonePage />} />
        <Route path="/order/:id" element={<OrderDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/checkout" element={<CheckoutPage />} />
        <Route path="/order-confirmation/:orderId" element={<OrderConfirmation />} />
        <Route path="/order-tracking/:orderId" element={<OrderTracking />} />
        <Route path="/orders/history" element={<OrderHistoryPage />} />
      </Routes>
    </ProductProvider>
  );
}

export default App;
