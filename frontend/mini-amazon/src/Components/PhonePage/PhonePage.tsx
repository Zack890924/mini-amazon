// src/Components/PhonePage/PhonePage.tsx
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import './PhonePage.css';
import Product from '../Product/Product';

const PhonePage: React.FC = () => {
  const [listOfProduct, setListOfProducts] = useState<{ id: number; name: string; rating: string; price: string; image: string; }[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filteredProducts, setFilteredProducts] = useState<{ id: number; name: string; rating: string; price: string; image: string; }[]>([]);

  useEffect(() => {
    const list = [
      {
        id: 123,
        name: 'Iphone 10',
        rating: '123',
        price: '40000',
        image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160'
      },
      {
        id: 456,
        name: 'Iphone 11',
        rating: '200',
        price: '45000',
        image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160'
      },
      {
        id: 789,
        name: 'Iphone 12',
        rating: '300',
        price: '50000',
        image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160'
      },
      {
        id: 101,
        name: 'Samsung Galaxy S21',
        rating: '250',
        price: '42000',
        image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160'
      },
      {
        id: 102,
        name: 'Google Pixel 6',
        rating: '280',
        price: '46000',
        image: 'https://ik.imagekit.io/amazonbbb11/amazon-image/mobiles/71ZOtNdaZCL._AC_UL640_FMwebp_QL65_.webp?updatedAt=1744058901160'
      }
    ];
      
    setListOfProducts(list);
    setFilteredProducts(list);
  }, []);

  // 搜索功能
  useEffect(() => {
    const results = listOfProduct.filter(product =>
      product.name.toLowerCase().includes(searchTerm.toLowerCase())
    );
    setFilteredProducts(results);
  }, [searchTerm, listOfProduct]);

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  return (
    <div className="phone-page__main">
      <h1>Phone Products</h1>
      
      {/* 搜索框 */}
      <div className="search-container">
        <input
          type="text"
          placeholder="Search products..."
          value={searchTerm}
          onChange={handleSearch}
          className="search-input"
        />
      </div>
      
      <div className="phone-page__products">
        {filteredProducts.length > 0 ? (
          filteredProducts.map((item) => (
            <Link to={`/order/${item.id}`} key={item.id}>
              <Product definition={item} />
            </Link>
          ))
        ) : (
          <p className="no-results">No products found matching your search.</p>
        )}
      </div>
    </div>
  );
};

export default PhonePage;