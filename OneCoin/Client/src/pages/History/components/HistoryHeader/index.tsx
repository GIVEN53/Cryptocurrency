import React from 'react';
import { HISTORY_PERIOD, HISTORY_TYPE } from 'utills/constants/investments';
import ButtonList from '../ButtonList';
import { Wrapper, SearchBox } from './style';

import { FaSearch } from 'react-icons/fa';

const HistoryHeader = () => {
	return (
		<Wrapper>
			<ButtonList title="기간" list={HISTORY_PERIOD} />
			<ButtonList title="종류" list={HISTORY_TYPE} />
			<SearchBox>
				<span>코인선택</span>
				<div>
					<FaSearch className="icon" />
					<input type="text" placeholder="전체" />
				</div>
			</SearchBox>
		</Wrapper>
	);
};

export default HistoryHeader;
