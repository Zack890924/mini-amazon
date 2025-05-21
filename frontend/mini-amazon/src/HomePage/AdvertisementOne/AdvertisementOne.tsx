import React from 'react';
import "./AdvertisementOne.css";
import { Link } from 'react-router-dom';

const AdvertisementOne: React.FC = () => {
  return (
    <div className="AdvertisementOne__main">
      <div className="AdvertisementOne__header">
        Pick up where you left off
      </div>
      <div className="AdvertisementOne__body">
        <img
          src="https://ik.imagekit.io/amazonbbb11/amazon-image/tab9.jpg?updatedAt=1744058899023"
          width="300px"
          alt="advertisement"
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

export default AdvertisementOne;
