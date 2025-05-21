import React from 'react';
import './Navbar.css';
import { Link, useNavigate } from 'react-router-dom';
import { useProductContext } from '../Contexts/ProductContext';

const Navbar: React.FC = () => {
  const { getCartCount } = useProductContext();
  const [searchTerm, setSearchTerm] = React.useState('');
  const navigate = useNavigate();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if(searchTerm.trim()){
      alert(`Searching for: ${searchTerm}`);
    }
  };

  return (
    <>
      <div className="navbar">
        <div className="navbar__left">
          <Link to="/">
            <div className="navbar__logo" />
          </Link>
          <div className="navbar__location">
            <div className="navbar__locationIcon">📍</div>
            <div className="navbar__locationText">
              <div className="navbar__textSmall">Deliver to</div>
              <div className="navbar__textBold">NC</div>
            </div>
          </div>
        </div>

        <form className="navbar__search" onSubmit={handleSearch}>
          <select className="navbar__searchDropdown">
            <option value="All">All</option>
            <option value="Electronics">Electronics</option>
            <option value="Phones">Phones</option>
          </select>
          <input 
            className="navbar__searchInput" 
            type="text" 
            placeholder="Search Mini-Amazon" 
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
          <button type="submit" className="navbar__searchButton">🔍</button>
        </form>

        <div className="navbar__links">
          <div className="navbar__link">
            <div className="navbar__textSmall">Hello, Sign In</div>
            <div className="navbar__textBold">Account & Lists</div>
          </div>
          <Link to="/orders/history" className="navbar__link">
            <div className="navbar__textSmall">Returns</div>
            <div className="navbar__textBold">& Orders</div>
          </Link>
          <Link to="/cart" className="navbar__cart">
            🛒 <span className="navbar__cartCount">{getCartCount()}</span>
          </Link>
        </div>
      </div>

      <div className="navbar__footer">
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
        <Link to="/phone" className="navbar__footer_text">Phones</Link>
      </div>
    </>
  );
};

export default Navbar;