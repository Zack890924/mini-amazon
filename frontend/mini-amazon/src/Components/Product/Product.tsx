import React from 'react';
import './Product.css';

interface ProductDefinition {
  id: number;
  name: string;
  rating: string;
  price: string;
  image: string;
}

interface ProductProps {
  definition: ProductDefinition;
}

const Product: React.FC<ProductProps> = ({ definition }) => {
  return (
    <div className="product">
      <img src={definition.image} alt={definition.name} className="product__image" />
      <div className="product__name">{definition.name}</div>
      <div className="product__rating">⭐ {definition.rating}</div>
      <div className="product__price">${definition.price}</div>
    </div>
  );
};

export default Product;
