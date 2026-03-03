// src/Homepage/AdvertisementEight/AdvertisementEight.tsx
import React from 'react';
import "../AdvertisementOne/AdvertisementOne.css";
import { Link } from 'react-router-dom';



const AdvertisementEight: React.FC = () => {
  return (
    <div className="AdvertisementOne__main">
      <div className="AdvertisementOne__header">
        Deal of the Day
      </div>
      <div className="AdvertisementOne__body">
        <img
          src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023"
          width="300px"
          alt="deal"
        />
      </div>
      <div className="AdvertisementOne__footer">
        <Link to="/phone" className="AdvertisementOne__footer-link">
          See more
        </Link>
      </div>
    </div>
  );
};

export default AdvertisementEight;
