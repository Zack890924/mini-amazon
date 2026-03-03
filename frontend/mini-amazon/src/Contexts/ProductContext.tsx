// src/Contexts/ProductContext.tsx
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';


export interface CartItem {
  id: number;
  name: string;
  price: string;
  image: string;
  quantity: number;
}

interface ProductContextType {
  cart: CartItem[];
  addToCart: (product: Omit<CartItem, 'quantity'>, quantity: number) => void;
  removeFromCart: (id: number) => void;
  updateQuantity: (id: number, quantity: number) => void;
  clearCart: () => void;
  getCartTotal: () => number;
  getCartCount: () => number;
}


const ProductContext = createContext<ProductContextType | undefined>(undefined);

export const useProductContext = () => {
  const context = useContext(ProductContext);
  if(!context){
    throw new Error('useProductContext must be used within a ProductProvider');
  }
  return context;
};




export const ProductProvider: React.FC<{ children: ReactNode }> = ({ children }) => {

  const [cart, setCart] = useState<CartItem[]>(() => {
    const savedCart = localStorage.getItem('cart');
    return savedCart ? JSON.parse(savedCart) : [];
  });


  useEffect(() => {
    localStorage.setItem('cart', JSON.stringify(cart));
    console.log('Cart updated:', cart);
  }, [cart]);


  const addToCart = (product: Omit<CartItem, 'quantity'>, quantity: number) => {
    console.log('Adding to cart:', product, quantity);
    
    setCart(prevCart => {

      const existingItem = prevCart.find(item => item.id === product.id);
      
      if(existingItem){

        return prevCart.map(item => 
          item.id === product.id 
            ? { ...item, quantity: item.quantity + quantity }
            : item
        );
      }
      else{
        return [...prevCart, { ...product, quantity }];
      }
    });
  };

  const removeFromCart = (id: number) => {
    setCart(prevCart => prevCart.filter(item => item.id !== id));
  };

  // 更新购物车中商品的数量
  const updateQuantity = (id: number, quantity: number) => {
    if(quantity <= 0){
      removeFromCart(id);
      return;
    }
    
    setCart(prevCart => 
      prevCart.map(item => 
        item.id === id ? { ...item, quantity } : item
      )
    );
  };


  const clearCart = () => {
    setCart([]);
  };


  const getCartTotal = () => {
    return cart.reduce((total, item) => {
      return total + (parseInt(item.price) * item.quantity);
    }, 0);
  };


  const getCartCount = () => {
    return cart.reduce((count, item) => count + item.quantity, 0);
  };

  const value = {
    cart,
    addToCart,
    removeFromCart,
    updateQuantity,
    clearCart,
    getCartTotal,
    getCartCount
  };

  return (
    <ProductContext.Provider value={value}>
      {children}
    </ProductContext.Provider>
  );
};

export default ProductContext;