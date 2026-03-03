// MainPage.tsx
import React from 'react';
import './MainPage.css';
import AdvertisementFour from './AdvertisementFour/AdvertiseMentFour';
import AdvertisementEight from './AdvertisementEight/AdvertisementEight';
import AdvertisementSeven from './AdvertisementSeven/AdvertisementSeven';
import AdvertisementOne from './AdvertisementOne/AdvertisementOne';


const MainPage: React.FC = () => {
  return (
    <div className='mainpage'>
      <div className='mainpage__container'>
        <AdvertisementOne />
        <AdvertisementFour />
        <AdvertisementSeven />
        <AdvertisementEight />
      </div>
    </div>
  );
};

export default MainPage;
