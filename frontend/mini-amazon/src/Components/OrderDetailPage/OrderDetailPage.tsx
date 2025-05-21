// src/Components/OrderDetailPage/OrderDetailPage.tsx
import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import './OrderDetailPage.css';
import { useProductContext } from '../../Contexts/ProductContext';

interface ProductType {
  id: number;
  name: string;
  rating: string;
  review: string;
  delivery: string;
  price: string;
  status: string;
  soldby: string;
  image: string;
  about: string[];
}

const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [product, setProduct] = useState<ProductType | null>(null);
  const [quantity, setQuantity] = useState(1);
  const { addToCart } = useProductContext();

  useEffect(() => {
    const products: ProductType[] = [
        {
          id: 123,
          name: 'Iphone 10',
          rating: '123',
          review: '1000',
          delivery: 'Thursday, April 11',
          price: '40000',
          status: 'In Stock',
          soldby: 'Amazon Seller',
          image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160',
          about: [
            'good phone'
          ]
        },
        {
          id: 456,
          name: 'Iphone 11',
          rating: '200',
          review: '2000',
          delivery: 'Friday, April 12',
          price: '45000',
          status: 'In Stock',
          soldby: 'Amazon Seller',
          image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160',
          about: [
            'good phone'
          ]
        },
        {
          id: 789,
          name: 'Iphone 12',
          rating: '300',
          review: '3000',
          delivery: 'Saturday, April 13',
          price: '50000',
          status: 'In Stock',
          soldby: 'Apple Store',
          image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160',
          about: [
            'good phone'
          ]
        },
        {
          id: 101,
          name: 'Samsung Galaxy S21',
          rating: '250',
          review: '2500',
          delivery: 'Sunday, April 14',
          price: '42000',
          status: 'In Stock',
          soldby: 'Samsung Official Store',
          image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160',
          about: [
            'good phone'
          ]
        },
        {
          id: 102,
          name: 'Google Pixel 6',
          rating: '280',
          review: '2800',
          delivery: 'Monday, April 15',
          price: '46000',
          status: 'In Stock',
          soldby: 'Google Store',
          image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160',
          about: [
            'good phone'
          ]
        }
      ];
        
      const found = products.find((p) => String(p.id) === id);
      if(found) setProduct(found);
    }, [id]);

  const handleAddToCart = () => {
    if(product){
      addToCart({
        id: product.id,
        name: product.name,
        price: product.price,
        image: product.image
      }, quantity);
      
      alert(`Added ${quantity} ${product.name}(s) to cart!`);
    }
  };

  const handleBuyNow = () => {
    if(product){
      addToCart({
        id: product.id,
        name: product.name,
        price: product.price,
        image: product.image
      }, quantity);
      
      // 跳转到结账页面
      window.location.href = '/checkout';
    }
  };

  const handleQuantityChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setQuantity(parseInt(e.target.value));
  };

  if(!product) return <div>Loading...</div>;

  return (
    <div className="order-detail-page">
      <div className="order-detail-left">
        <img src={product.image} alt={product.name} />
      </div>

      <div className="order-detail-center">
        <h2>{product.name}</h2>
        <div className="order-detail-rating">⭐ {product.rating} | {product.review} reviews</div>
        <hr />
        <div className="order-detail-info">
          <p><strong>Price:</strong> <span className="price-tag">${product.price}</span></p>
          <p><strong>Delivery:</strong> {product.delivery}</p>
          <p className="order-detail-status">{product.status}</p>
          <p>Sold by <strong>{product.soldby}</strong></p>
        </div>
        <h4>About this item</h4>
        <ul>
          {product.about.map((line, index) => (
            <li key={index}>{line}</li>
          ))}
        </ul>
      </div>

      <div className="order-detail-right">
        <p><strong>Without Exchange</strong></p>
        <p className="price-tag">${product.price}</p>
        
        <div className="quantity-selector">
          <label htmlFor="quantity"><strong>Quantity:</strong></label>
          <select 
            id="quantity" 
            value={quantity} 
            onChange={handleQuantityChange}
            className="quantity-select"
          >
            {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(num => (
              <option key={num} value={num}>{num}</option>
            ))}
          </select>
        </div>
        
        <button className="btn add" onClick={handleAddToCart}>Add to Cart</button>
        <button className="btn buy" onClick={handleBuyNow}>Buy Now</button>
      </div>
    </div>
  );
};

export default OrderDetailPage;