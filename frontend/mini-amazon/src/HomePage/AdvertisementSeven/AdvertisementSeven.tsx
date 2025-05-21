// src/Homepage/AdvertisementSeven/AdvertisementSeven.tsx
import React from 'react';
import "../AdvertisementOne/AdvertisementOne.css";
import { Link } from 'react-router-dom';


const AdvertisementSeven: React.FC = () => {
  return (
    <div className="AdvertisementOne__main">
      <div className="AdvertisementOne__header">
        Trending Picks
      </div>
      <div className="AdvertisementFour__body">
        <img src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023" alt="1" />
        <img src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023" alt="2" />
        <img src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023" alt="3" />
        <img src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023" alt="4" />
      </div>
      <div className="AdvertisementFour__footer">
        <Link to="/phone" className="AdvertisementFour__footer-link">
          See more
        </Link>
      </div>
    </div>
  );
};

export default AdvertisementSeven;
